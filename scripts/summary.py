from pathlib import Path
import glob
import html
import os
import sys
import tempfile
import xml.etree.ElementTree as ET


def read_results(pattern):
    files = sorted(glob.glob(pattern, recursive=True))
    if not files:
        raise ValueError('JUnit ausente')
    cases = [case for name in files for case in ET.parse(name).iter('testcase')]
    if not cases:
        raise ValueError('JUnit vazio')
    return [(case.get('name', 'sem nome'),
             'falhou' if case.find('failure') is not None or case.find('error') is not None or case.get('status') == 'FAILURE'
             else 'ignorado' if case.find('skipped') is not None or case.get('status') == 'SKIPPED' else 'passou',
             case.get('time', '-')) for case in cases]


def approved(cases, outcome, expected):
    return outcome == 'success' and len(cases) == expected and all(status == 'passou' for _, status, _ in cases)


def cell(value):
    return html.escape(str(value)).replace('|', '&#124;').replace('\n', ' ').replace('\r', ' ')


def summary(cases, outcome, expected, problem=''):
    counts = {status: sum(c[1] == status for c in cases) for status in ('passou', 'falhou', 'ignorado')}
    ok = not problem and approved(cases, outcome, expected)
    reasons = [problem] if problem else []
    if outcome != 'success': reasons.append(f'Etapa de testes: {outcome}; confira os steps anteriores.')
    if len(cases) != expected: reasons.append(f'Quantidade incompleta ou inesperada: {len(cases)}/{expected}.')
    if counts['falhou'] or counts['ignorado']: reasons.append('Há falhas, erros ou cenários ignorados.')
    lines = ['## ' + cell(os.environ.get('SUMMARY_TITLE', 'Resultados dos testes')), '',
             f"Gate: **{'aprovado' if ok else 'reprovado'}**. Etapa: **{cell(outcome)}**.", '',
             '| Esperados | Registrados | Aprovados | Falhas/erros | Ignorados |', '| ---: | ---: | ---: | ---: | ---: |',
             f"| {expected} | {len(cases)} | {counts['passou']} | {counts['falhou']} | {counts['ignorado']} |", '',
             cell(' '.join(reasons)), '', '| Cenário | Resultado | Duração (s) |', '| --- | --- | ---: |']
    lines += [f'| {cell(name)} | {status} | {cell(duration)} |' for name, status, duration in cases]
    lines += ['', cell(os.environ.get('TEST_SCOPE', 'Escopo não informado.')), '',
              'JUnit e este resumo estão no artifact da run. Uma interrupção pode impedir o registro de todos os cenários.']
    if os.environ.get('GITHUB_SHA'):
        lines += ['', 'Commit: `' + cell(os.environ['GITHUB_SHA']) + '`.']
    return '\n'.join(lines) + '\n', ok


def self_test():
    with tempfile.TemporaryDirectory() as directory:
        report = Path(directory) / 'junit.xml'
        for content in (None, '', '<testsuite/>'):
            if content is not None: report.write_text(content)
            try: read_results(str(report))
            except (ValueError, ET.ParseError): pass
            else: raise AssertionError('Missing, invalid or empty report was accepted')
        report.write_text('<testsuite><testcase name="ok" time="0.1"/><testcase name="bad"><failure/></testcase><testcase name="error"><error/></testcase><testcase name="skip"><skipped/></testcase><testcase name="maestro" status="FAILURE"/></testsuite>')
        cases = read_results(str(report))
        assert [c[1] for c in cases] == ['passou', 'falhou', 'falhou', 'ignorado', 'falhou']
        assert not summary(cases, 'success', 5)[1]
        valid = [('name|<tag>\nnext', 'passou', '0.1')]
        assert summary(valid, 'success', 1)[1]
        for outcome, expected, problem in [('failure', 1, ''), ('skipped', 1, ''), ('success', 2, ''), ('success', 1, 'Invalid report')]:
            assert not summary(valid, outcome, expected, problem)[1]
        assert not summary([('skip', 'ignorado', '0')], 'success', 1)[1]
        assert 'name&#124;&lt;tag&gt; next' in summary(valid, 'success', 1)[0]
    print('Summary parser and gate checks passed')


if __name__ == '__main__':
    if '--self-test' in sys.argv:
        self_test()
        raise SystemExit(0)
    cases, problem = [], ''
    try:
        cases = read_results(os.environ.get('JUNIT_PATTERN', 'results/junit.xml'))
    except (OSError, ValueError, ET.ParseError) as error:
        problem = f'JUnit ausente, inválido ou vazio ({type(error).__name__}).'
    content, ok = summary(cases, os.environ.get('TEST_OUTCOME', 'unknown'), int(os.environ['EXPECTED_TESTS']), problem)
    output = Path(os.environ.get('SUMMARY_FILE', 'results/summary.md'))
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(content, encoding='utf-8')
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a', encoding='utf-8') as stream:
            stream.write(content)
    print(content)
    raise SystemExit(0 if ok else 1)
