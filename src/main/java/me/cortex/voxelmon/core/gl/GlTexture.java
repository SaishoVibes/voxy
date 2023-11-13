package me.cortex.voxelmon.core.gl;

import me.cortex.voxelmon.core.util.TrackedObject;

import static org.lwjgl.opengl.ARBFramebufferObject.glDeleteFramebuffers;
import static org.lwjgl.opengl.ARBFramebufferObject.glGenFramebuffers;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.glDeleteTextures;
import static org.lwjgl.opengl.GL45C.glCreateTextures;
import static org.lwjgl.opengl.GL45C.glTextureStorage2D;

public class GlTexture extends TrackedObject {
    final int id;
    private final int type;
    public GlTexture(int type) {
        this.id = glCreateTextures(type);
        this.type = type;
    }

    public GlTexture store(int format, int levels, int width, int height) {
        if (this.type == GL_TEXTURE_2D) {
            glTextureStorage2D(this.id, format, levels, width, height);
        } else {
            throw new IllegalStateException("Unknown texture type");
        }
        return this;
    }

    @Override
    public void free() {
        super.free0();
        glDeleteTextures(this.id);
    }
}
