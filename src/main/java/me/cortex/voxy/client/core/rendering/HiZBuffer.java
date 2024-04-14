package me.cortex.voxy.client.core.rendering;

import me.cortex.voxy.client.core.gl.GlFramebuffer;
import me.cortex.voxy.client.core.gl.GlTexture;
import me.cortex.voxy.client.core.gl.shader.Shader;
import me.cortex.voxy.client.core.gl.shader.ShaderType;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.ARBDirectStateAccess.*;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL33.glBindSampler;
import static org.lwjgl.opengl.GL33.glGenSamplers;
import static org.lwjgl.opengl.GL33C.glDeleteSamplers;
import static org.lwjgl.opengl.GL45C.glNamedFramebufferTexture;

public class HiZBuffer {
    private final Shader hiz = Shader.make()
            .add(ShaderType.VERTEX, "voxy:hiz/blit.vsh")
            .add(ShaderType.FRAGMENT, "voxy:hiz/blit.fsh")
            .compile();
    private final GlFramebuffer fb = new GlFramebuffer();
    private final int sampler = glGenSamplers();
    private GlTexture texture;
    private int levels;
    private int width;
    private int height;

    public HiZBuffer() {
        glNamedFramebufferDrawBuffer(this.fb.id, GL_NONE);
    }

    private void alloc(int width, int height) {
        this.levels = (int)Math.ceil(Math.log(Math.max(width, height))/Math.log(2));
        //We dont care about e.g. 1x1 size texture since you dont get meshlets that big to cover such a large area
        this.levels -= 3;//Arbitrary size, shinks the max level by alot and saves a significant amount of processing time
        // (could probably increase it to be defined by a max meshlet coverage computation thing)

        //We do a hack where we assume (which is probably wrong) that we will on average never
        // get a meshlet that uses 0 mipping
        this.levels--;
        this.texture = new GlTexture().store(GL_DEPTH_COMPONENT32, this.levels, width/2, height/2);
        glTextureParameteri(this.texture.id, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTextureParameteri(this.texture.id, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTextureParameteri(this.texture.id, GL_TEXTURE_COMPARE_MODE, GL_NONE);
        glTextureParameteri(this.texture.id, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTextureParameteri(this.texture.id, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        this.width  = width;
        this.height = height;
    }

    public void buildMipChain(int srcDepthTex, int width, int height) {
        if (this.width != width || this.height != height) {
            if (this.texture != null) {
                this.texture.free();
                this.texture = null;
            }
            this.alloc(width, height);
        }
        glBindVertexArray(AbstractFarWorldRenderer.STATIC_VAO);
        int boundFB = GL11.glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        this.hiz.bind();
        this.fb.bind(GL_DEPTH_ATTACHMENT, this.texture, 0).verify();
        glBindFramebuffer(GL_FRAMEBUFFER, this.fb.id);

        glDepthFunc(GL_ALWAYS);

        glBindTextureUnit(0, srcDepthTex);
        glBindSampler(0, this.sampler);
        glUniform1i(0, 0);
        int cw = this.width /2;
        int ch = this.height/2;
        for (int i = 0; i < this.levels; i++) {
            this.fb.bind(GL_DEPTH_ATTACHMENT, this.texture, i);
            glViewport(0, 0, cw, ch); cw /= 2; ch /= 2;
            glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
            if (i == 0) {
                glBindTextureUnit(0, this.texture.id);
            }
            glTextureParameteri(this.texture.id, GL_TEXTURE_BASE_LEVEL, i);
            glTextureParameteri(this.texture.id, GL_TEXTURE_MAX_LEVEL, i+1);
        }
        glTextureParameteri(this.texture.id, GL_TEXTURE_BASE_LEVEL, 0);
        glTextureParameteri(this.texture.id, GL_TEXTURE_MAX_LEVEL, this.levels-1);//TODO: CHECK IF ITS -1 or -0

        glDepthFunc(GL_LEQUAL);
        glBindFramebuffer(GL_FRAMEBUFFER, boundFB);
        glViewport(0, 0, width, height);
        glBindVertexArray(0);
    }

    public void free() {
        this.fb.free();
        this.texture.free();
        this.texture = null;
        glDeleteSamplers(this.sampler);
    }
}
