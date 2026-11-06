package com.HyperX.MI.Na.liquid

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import com.kyant.backdrop.Backdrop as KyantBackdrop
import top.yukonga.miuix.kmp.blur.Backdrop as MiuixBackdrop

@Stable
class MiuixBackdropAdapter(
    private val delegate: KyantBackdrop,
) : MiuixBackdrop {

    override val isCoordinatesDependent: Boolean
        get() = delegate.isCoordinatesDependent

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Int,
    ) {
        with(delegate) {
            drawBackdrop(density, coordinates, layerBlock)
        }
    }
}
