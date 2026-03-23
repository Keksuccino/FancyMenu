package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import com.google.gson.annotations.SerializedName;

public enum AfmaFrameOperationType {

    @SerializedName("full")
    FULL,
    @SerializedName("delta_rect")
    DELTA_RECT,
    @SerializedName("sparse_delta_rect")
    SPARSE_DELTA_RECT,
    @SerializedName("copy_rect_sparse_patch")
    COPY_RECT_SPARSE_PATCH,
    @SerializedName("same")
    SAME,
    @SerializedName("copy_rect_patch")
    COPY_RECT_PATCH

}
