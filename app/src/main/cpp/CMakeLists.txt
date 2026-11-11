# MINova native 热点路径（libminova.so）
# 纯计算，无 Android 依赖：波浪预计算 / LRC 解析 / FNV-1a 指纹 /
# 歌曲过滤（isLikelyMusic / isJunkVersion）/ 去重 key。
# Kotlin 侧入口见 com.HyperX.MI.Na.native.NativeBridge（不可用时自动走 Kotlin 兜底）。
cmake_minimum_required(VERSION 3.22.1)
project(minova CXX)

add_library(minova SHARED minova.cpp)

target_compile_features(minova PRIVATE cxx_std_17)

# 逐位对齐 Kotlin/ART 浮点语义：禁 FMA 收缩、禁隐藏可见性外的符号暴露。
# （ART 按字节码逐运算舍入，C++ 若允许 a*b+c 收缩成 fma 会产生 ±1ulp 差异，
#  波浪路径要求与原 Kotlin 循环视觉完全一致，故必须 -ffp-contract=off。）
target_compile_options(minova PRIVATE -ffp-contract=off -fvisibility=hidden)

# ── Release 瘦身（最低占用）─────────────────────────────────────────────
# ⚠️ AGP 9 的 release variant 映射为 CMAKE_BUILD_TYPE=RelWithDebInfo（非 Release），
# 因此用「非 Debug」判定，Debug 构建保持原样便于断点调试。
# -Os            : 体积优先（目标选项在命令行靠后，覆盖 AGP 的 -O2）
# -f*-sections + --gc-sections : 未被引用的函数/数据段整段剔除
# -fno-exceptions / -fno-rtti : 本库无 throw/try/catch、无 dynamic_cast/typeid，
#                   去掉 EH 表与 RTTI 元数据（std::string/vector 正常使用；
#                   仅在 OOM 等 abort 路径上由抛异常改为终止，纯计算场景无影响）
# -fno-unwind-tables -fno-asynchronous-unwind-tables :
#                   -fno-exceptions 下异常不需要穿过本库，unwind 表可去
#                   （代价：仅本库的崩溃回溯精度下降，tombstone 仍定位到 so）
target_compile_options(minova PRIVATE
    $<$<NOT:$<CONFIG:Debug>>:-Os;-ffunction-sections;-fdata-sections;-fno-exceptions;-fno-rtti;-fno-unwind-tables;-fno-asynchronous-unwind-tables>)
target_link_options(minova PRIVATE
    $<$<NOT:$<CONFIG:Debug>>:-Wl,--gc-sections>)
