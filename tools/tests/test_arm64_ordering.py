import importlib.util
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("ordering", ROOT / "scripts/prepare-arm64-ordering.py")
ordering = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ordering)


class OrderingTests(unittest.TestCase):
    def test_instructions_only(self):
        source = "void f() {\r\n\t// sync \r\n\t// lwsync\r\n\t// eieio \r\n\t// sync is documented here\r\n}\r\n"
        fixed, count = ordering.ordered_source(source)
        self.assertEqual(count, 3)
        self.assertEqual(fixed.count("__atomic_thread_fence(__ATOMIC_SEQ_CST);"), 3)
        self.assertIn("// sync is documented here\r\n}", fixed)

    def test_copies_preserve_upstream_and_do_not_rewrite_outputs(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "upstream"
            source.mkdir()
            original = source / "simpsons_recomp.0.cpp"
            original.write_text("void f() {\n  // eieio \n}\n", encoding="utf-8")
            plain = source / "simpsons_recomp.1.cpp"
            plain.write_text("void g() {}\n", encoding="utf-8")
            saved = original.read_bytes()
            output = root / "android"
            ordering.prepare(source, output)
            copy = output / original.name
            before = copy.stat().st_mtime_ns
            ordering.prepare(source, output)
            self.assertEqual(original.read_bytes(), saved)
            self.assertEqual(copy.stat().st_mtime_ns, before)
            self.assertFalse((output / plain.name).exists())
            self.assertIn(original.name, (output / "sources.cmake").read_text())
            # Removing a barrier must also remove the copy from the build list.
            original.write_text("void f() {}\n", encoding="utf-8")
            ordering.prepare(source, output)
            self.assertNotIn(original.name, (output / "sources.cmake").read_text())


if __name__ == "__main__":
    unittest.main()
