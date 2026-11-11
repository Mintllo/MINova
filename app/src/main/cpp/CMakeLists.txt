# MINova native 热点路径（libminova.so）
# 纯计算，无 Android 依赖：波浪预计算 / LRC 解析 / FNV-1a 指纹。
# Kotlin 侧入口见 com.HyperX.MI.Na.native.NativeBridge（不可用时自动走 Kotlin 兜底）。
cmake_minimum_required(VERSION 3.22.1)
project(minova CXX)

add_library(minova SHARED minova.cpp)

target_compile_features(minova PRIVATE cxx_std_17)

# 逐位对齐 Kotlin/ART 浮点语义：禁 FMA 收缩、禁隐藏可见性外的符号暴露。
# （ART 按字节码逐运算舍入，C++ 若允许 a*b+c 收缩成 fma 会产生 ±1ulp 差异，
#  波浪路径要求与原 Kotlin 循环视觉完全一致，故必须 -ffp-contract=off。）
target_compile_options(minova PRIVATE -ffp-contract=off -fvisibility=hidden)
