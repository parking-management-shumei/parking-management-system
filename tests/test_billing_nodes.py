"""Offline tests extract pure billing code; no Coze runtime or external credentials required."""
import ast
import unittest
from pathlib import Path

SOURCE = Path(__file__).resolve().parents[1] / "coze/charge/Parking_charging_system.py"
tree = ast.parse(SOURCE.read_text(encoding="utf-8"))
allowed = [n for n in tree.body if isinstance(n, (ast.Import, ast.ImportFrom)) and not (isinstance(n, ast.ImportFrom) and (n.module == "runtime" or n.module.startswith("typings.")))]
allowed += [n for n in tree.body if isinstance(n, ast.FunctionDef) and n.name == "calculate_price"]
namespace = {}
exec(compile(ast.Module(body=allowed, type_ignores=[]), str(SOURCE), "exec"), namespace)
calculate = namespace["calculate_price"]

class BillingTests(unittest.TestCase):
    def test_matches_java_fee_contract(self):
        for seconds, expected in [(0,0),(900,0),(901,5),(3600,5),(3601,8),(86400,20),(86401,25),(172800,40)]:
            with self.subTest(seconds=seconds):
                self.assertEqual(calculate(seconds, 5, 20, 3, 60, 15), expected)
    def test_rejects_invalid_inputs(self):
        for seconds, unit in [(-1,60),(3600,0),(3600,59)]:
            with self.assertRaises(ValueError): calculate(seconds, 5, 20, 3, unit, 15)
        with self.assertRaises(ValueError): calculate(3600, float('nan'), 20, 3, 60, 15)
    def test_all_node_exports_parse(self):
        for path in SOURCE.parents[1].rglob('*.py'):
            ast.parse(path.read_text(encoding='utf-8'),filename=str(path))

if __name__ == '__main__': unittest.main()
