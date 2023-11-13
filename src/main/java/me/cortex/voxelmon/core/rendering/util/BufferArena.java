package me.cortex.voxelmon.core.rendering.util;

import me.cortex.voxelmon.core.gl.GlBuffer;
import me.cortex.voxelmon.core.util.AllocationArena;
import me.cortex.voxelmon.core.util.MemoryBuffer;
import org.lwjgl.system.MemoryUtil;

public class BufferArena {
    private final long size;
    private final int elementSize;
    private final GlBuffer buffer;
    private final AllocationArena allocationMap = new AllocationArena();
    private long used;

    public BufferArena(long capacity, int elementSize) {
        if (capacity%elementSize != 0) {
            throw new IllegalArgumentException("Capacity not a multiple of element size");
        }
        this.size = capacity;
        this.elementSize = elementSize;
        this.buffer = new GlBuffer(capacity, 0);
        this.allocationMap.setLimit(capacity/elementSize);
    }

    public long upload(MemoryBuffer buffer) {
        if (buffer.size%this.elementSize!=0) {
            throw new IllegalArgumentException("Buffer size not multiple of elementSize");
        }
        int size = (int) (buffer.size/this.elementSize);
        long addr = this.allocationMap.alloc(size);
        long uploadPtr = UploadStream.INSTANCE.upload(this.buffer, addr * this.elementSize, buffer.size);
        MemoryUtil.memCopy(buffer.address, uploadPtr, buffer.size);
        this.used += size;
        return addr;
    }

    public void free(long allocation) {
        this.used -= this.allocationMap.free(allocation);
    }

    public void free() {
        this.buffer.free();
    }

    public int id() {
        this.buffer.assertNotFreed();
        return this.buffer.id;
    }

    public float usage() {
        return (float) ((double)this.used/(this.buffer.size()/this.elementSize));
    }
}
