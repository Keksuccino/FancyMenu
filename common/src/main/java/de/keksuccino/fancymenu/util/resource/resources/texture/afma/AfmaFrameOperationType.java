package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import com.google.gson.annotations.SerializedName;

public enum AfmaFrameOperationType {

    @SerializedName("full")
    FULL,
    @SerializedName("delta_rect")
    DELTA_RECT,
    @SerializedName("residual_delta_rect")
    RESIDUAL_DELTA_RECT,
    @SerializedName("sparse_delta_rect")
    SPARSE_DELTA_RECT,
    @SerializedName("same")
    SAME,
    @SerializedName("copy_rect_patch")
    COPY_RECT_PATCH,
    @SerializedName("copy_rect_residual_patch")
    COPY_RECT_RESIDUAL_PATCH,
    @SerializedName("copy_rect_sparse_patch")
    COPY_RECT_SPARSE_PATCH,
    @SerializedName("multi_copy_patch")
    MULTI_COPY_PATCH,
    @SerializedName("multi_copy_residual_patch")
    MULTI_COPY_RESIDUAL_PATCH,
    @SerializedName("multi_copy_sparse_patch")
    MULTI_COPY_SPARSE_PATCH,
    @SerializedName("block_inter")
    BLOCK_INTER

}
