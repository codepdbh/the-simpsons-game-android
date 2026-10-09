#include <jni.h>
#include <vulkan/vulkan.h>
#include <rex/ui/vulkan/android_gpu_driver.h>
#include <dlfcn.h>
#include <string>
#include <vector>
#include <cstring>
namespace {
std::string quote(const char* text) {
  std::string out="\"";
  for(unsigned char c:std::string(text)) {if(c=='"'||c=='\\') out+='\\';if(c>=32) out+=char(c);}
  return out+'"';
}
std::string probe(const char* hooks,const char* temp,const char* dir,const char* library) {
  struct Guard {
    void* library{}; VkInstance instance{}; PFN_vkDestroyInstance destroy{};
    ~Guard() {if(instance&&destroy) destroy(instance,nullptr);if(library) dlclose(library);}
  } guard;
  guard.library=rex_android_open_vulkan(hooks,temp,dir,library);
  if(!guard.library) return "{\"probeError\":\"No se pudo abrir el driver seleccionado\"}";
  auto get=reinterpret_cast<PFN_vkGetInstanceProcAddr>(dlsym(guard.library,"vkGetInstanceProcAddr"));
  if(!get) return "{\"probeError\":\"Driver sin vkGetInstanceProcAddr\"}";
  auto enumerateVersion=reinterpret_cast<PFN_vkEnumerateInstanceVersion>(get(nullptr,"vkEnumerateInstanceVersion"));
  uint32_t version=VK_API_VERSION_1_0; if(enumerateVersion) enumerateVersion(&version);
  if(version<VK_API_VERSION_1_1) return "{\"compatible\":false,\"probeError\":\"Se necesita Vulkan 1.1\"}";
  auto create=reinterpret_cast<PFN_vkCreateInstance>(get(nullptr,"vkCreateInstance"));
  if(!create) return "{\"probeError\":\"Driver sin vkCreateInstance\"}";
  VkApplicationInfo app{VK_STRUCTURE_TYPE_APPLICATION_INFO};app.pApplicationName="TSG GPU probe";app.apiVersion=VK_API_VERSION_1_1;
  VkInstanceCreateInfo info{VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO};info.pApplicationInfo=&app;
  if(create(&info,nullptr,&guard.instance)!=VK_SUCCESS) return "{\"probeError\":\"No se pudo iniciar Vulkan 1.1\"}";
  guard.destroy=reinterpret_cast<PFN_vkDestroyInstance>(get(guard.instance,"vkDestroyInstance"));
#define LOAD(name) auto name=reinterpret_cast<PFN_##name>(get(guard.instance,#name));if(!name) return "{\"probeError\":\"Driver Vulkan incompleto\"}"
  LOAD(vkEnumeratePhysicalDevices);LOAD(vkGetPhysicalDeviceProperties);LOAD(vkGetPhysicalDeviceFeatures);
  LOAD(vkGetPhysicalDeviceQueueFamilyProperties);LOAD(vkEnumerateDeviceExtensionProperties);
  LOAD(vkCreateDevice);LOAD(vkGetDeviceProcAddr);
#undef LOAD
  uint32_t count=0;if(vkEnumeratePhysicalDevices(guard.instance,&count,nullptr)!=VK_SUCCESS||!count) return "{\"probeError\":\"No se encontró una GPU\"}";
  std::vector<VkPhysicalDevice> devices(count);
  if(vkEnumeratePhysicalDevices(guard.instance,&count,devices.data())!=VK_SUCCESS) return "{\"probeError\":\"No se pudo enumerar la GPU\"}";
  auto gpu=devices[0];VkPhysicalDeviceProperties properties{};VkPhysicalDeviceFeatures f{};
  vkGetPhysicalDeviceProperties(gpu,&properties);vkGetPhysicalDeviceFeatures(gpu,&f);
  std::vector<std::string> missing;
  if(properties.apiVersion<VK_API_VERSION_1_1) missing.emplace_back("Vulkan 1.1");
  if(!f.independentBlend) missing.emplace_back("independentBlend");
  if(!f.fragmentStoresAndAtomics) missing.emplace_back("fragmentStoresAndAtomics");
  if(!f.vertexPipelineStoresAndAtomics) missing.emplace_back("vertexPipelineStoresAndAtomics");
  uint32_t ec=0;vkEnumerateDeviceExtensionProperties(gpu,nullptr,&ec,nullptr);std::vector<VkExtensionProperties> ext(ec);
  bool swapchain=false;
  if(vkEnumerateDeviceExtensionProperties(gpu,nullptr,&ec,ext.data())==VK_SUCCESS)
    for(const auto& e:ext) if(!std::strcmp(e.extensionName,VK_KHR_SWAPCHAIN_EXTENSION_NAME)) swapchain=true;
  if(!swapchain) missing.emplace_back("VK_KHR_swapchain");
  uint32_t qc=0;vkGetPhysicalDeviceQueueFamilyProperties(gpu,&qc,nullptr);std::vector<VkQueueFamilyProperties> queues(qc);
  vkGetPhysicalDeviceQueueFamilyProperties(gpu,&qc,queues.data());uint32_t family=qc;
  for(uint32_t i=0;i<qc;i++) if((queues[i].queueFlags&3)==3) {family=i;break;}
  if(family==qc) missing.emplace_back("Cola graphics/compute");
  if(missing.empty()) {
    float priority=1;VkDeviceQueueCreateInfo queue{VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO};
    queue.queueFamilyIndex=family;queue.queueCount=1;queue.pQueuePriorities=&priority;
    VkPhysicalDeviceFeatures enabled{};enabled.independentBlend=1;enabled.fragmentStoresAndAtomics=1;enabled.vertexPipelineStoresAndAtomics=1;
    const char* extension=VK_KHR_SWAPCHAIN_EXTENSION_NAME;VkDeviceCreateInfo di{VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO};
    di.queueCreateInfoCount=1;di.pQueueCreateInfos=&queue;di.pEnabledFeatures=&enabled;di.enabledExtensionCount=1;di.ppEnabledExtensionNames=&extension;
    VkDevice device{};auto result=vkCreateDevice(gpu,&di,nullptr,&device);
    if(result==VK_SUCCESS) {auto destroy=reinterpret_cast<PFN_vkDestroyDevice>(vkGetDeviceProcAddr(device,"vkDestroyDevice"));if(destroy) destroy(device,nullptr);}
    else missing.emplace_back("vkCreateDevice: "+std::to_string(result));
  }
  std::string out="{\"gpu\":"+quote(properties.deviceName)+",\"vulkan\":"+quote((std::to_string(VK_VERSION_MAJOR(properties.apiVersion))+"."+std::to_string(VK_VERSION_MINOR(properties.apiVersion))).c_str())+",\"compatible\":"+(missing.empty()?"true":"false")+",\"missing\":[";
  for(size_t i=0;i<missing.size();i++) {if(i) out+=',';out+=quote(missing[i].c_str());}return out+"]}";
}
}
extern "C" JNIEXPORT jstring JNICALL Java_org_tsg_android_GpuDiagnostics_nativeReport(JNIEnv* env,jclass,jstring hooks,jstring temp,jstring directory,jstring library) {
  const char* h=env->GetStringUTFChars(hooks,nullptr);const char* t=env->GetStringUTFChars(temp,nullptr);
  const char* d=env->GetStringUTFChars(directory,nullptr);const char* l=env->GetStringUTFChars(library,nullptr);
  std::string result=probe(h,t,d,l);
  env->ReleaseStringUTFChars(hooks,h);env->ReleaseStringUTFChars(temp,t);env->ReleaseStringUTFChars(directory,d);env->ReleaseStringUTFChars(library,l);
  return env->NewStringUTF(result.c_str());
}
