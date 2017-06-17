/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2014, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.vm.domain.data;

import lcmc.common.domain.StringValue;

/** Class that holds data about virtual video devices. */
public final class VideoData extends HardwareData {
    private final String modelType;
    /** Model type: cirrus, vga, vmvga, xen. */
    public static final String MODEL_TYPE = "model_type";
    public static final String SAVED_MODEL_TYPE = "saved_model_type";
    public static final String MODEL_VRAM = "model_vram";
    /** Model heads: 1. */
    public static final String MODEL_HEADS = "model_heads";

    public VideoData(final String modelType, final String modelVRAM, final String modelHeads) {
        super();
        this.modelType = modelType;
        setValue(MODEL_TYPE, new StringValue(modelType));
        setValue(MODEL_VRAM, new StringValue(modelVRAM));
        setValue(MODEL_HEADS, new StringValue(modelHeads));
    }

    public String getModelType() {
        return modelType;
    }
}
