#include <bit>
#include <cstdint>
#include <cstdio>
#include <rex/cvar.h>
#include <rex/graphics/pipeline/shader/shader.h>
#include <rex/string.h>

REXCVAR_DEFINE_STRING(dump_shaders, "", "GPU", "Shader dump disabled in regression test");

// Synthetic Xenos microcode: full zero-stride fetch, mini inheriting its slot,
// then another full fetch of the same slot with a different stride.
// No game microcode or assets are used by this regression test.
int main() {
  constexpr uint32_t full_slot_7 = (1u << 19) | (2u << 20) | (1u << 25);
  constexpr uint32_t mini_raw_slot_89 = (1u << 19) | (29u << 20) | (2u << 25);
  constexpr uint32_t xyz_float = (57u << 16) | 0xe88;
  const uint32_t code[] = {
      1u | (3u << 12) | (0x15u << 16), 2u << 12, 0,
      full_slot_7, xyz_float, 0,
      mini_raw_slot_89 | (1u << 12), xyz_float | (1u << 30), 255,
      full_slot_7 | (2u << 12), xyz_float, 4,
  };
  rex::graphics::Shader shader(rex::graphics::xenos::ShaderType::kVertex, 0,
                               code, sizeof(code) / sizeof(code[0]), std::endian::native);
  rex::string::StringBuffer disassembly;
  shader.AnalyzeUcode(disassembly);
  const auto& bindings = shader.vertex_bindings();
  if (bindings.size() != 2 || bindings[0].fetch_constant != 7 ||
      bindings[0].stride_words != 0 || bindings[0].attributes.size() != 2 ||
      bindings[1].fetch_constant != 7 || bindings[1].stride_words != 4 ||
      bindings[1].attributes.size() != 1) {
    std::fputs("FAIL: zero stride, inherited mini slot or mixed strides\n", stderr);
    return 1;
  }
  const auto& constants = shader.constant_register_map();
  if (!(constants.vertex_fetch_bitmap[0] & (1u << 7)) ||
      (constants.vertex_fetch_bitmap[2] & (1u << (89 - 64)))) {
    std::fputs("FAIL: mini raw slot leaked into residency map\n", stderr);
    return 1;
  }
  std::puts("PASS: zero-stride full fetch; mini inherits slot; mixed strides; residency map");
  return 0;
}
