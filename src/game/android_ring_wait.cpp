// The Android SDK lacks the newer GPU progress notification API. Preserve the
// original guest wait/timeout checks and yield for 1 ms only while still waiting.
#include <chrono>
#include <thread>
#include <rex/cvar.h>
#include <rex/ppc.h>

REXCVAR_DEFINE_BOOL(ring_wait_sleep, true, "GPU", "Yield while waiting for GPU command ring consumption");
REX_EXTERN(__imp__sub_82452018);
REX_EXTERN(sub_82452018);

REX_FUNC(sub_82452018) {
  __imp__sub_82452018(ctx, base);
  if (ctx.r3.u32 == 1 && REXCVAR_GET(ring_wait_sleep)) {
    std::this_thread::sleep_for(std::chrono::milliseconds(1));
  }
}
