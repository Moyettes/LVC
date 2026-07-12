package com.moyettes.legacyvoicechat.utils;

import com.mojang.blaze3d.platform.MemoryTracker;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class Tessellator {
	private static boolean convertQuadsToTriangles = true;
	private static boolean tryVBO = false;
	private ByteBuffer byteBuffer;
	private IntBuffer intBuffer;
	private FloatBuffer floatBuffer;
	private int[] rawBuffer;
	private int vertexCount = 0;
	private double textureU;
	private double textureV;
	private int color;
	private boolean hasColor = false;
	private boolean hasTexture = false;
	private boolean hasNormals = false;
	private int rawBufferIndex = 0;
	private int addedVertices = 0;
	private boolean isColorDisabled = false;
	private int drawMode;
	private double xOffset;
	private double yOffset;
	private double zOffset;
	private int normal;
	public static final Tessellator instance = new Tessellator(2097152);
	private boolean isDrawing = false;
	private boolean useVBO = false;
	private IntBuffer vertexBuffers;
	private int vboIndex = 0;
	private int vboCount = 10;
	private int bufferSize;

	private Tessellator(int i) {
		this.bufferSize = i;
		this.byteBuffer = MemoryTracker.createByteBuffer(i * 4);
		this.intBuffer = this.byteBuffer.asIntBuffer();
		this.floatBuffer = this.byteBuffer.asFloatBuffer();
		this.rawBuffer = new int[i];
		this.useVBO = tryVBO && GLContext.getCapabilities().GL_ARB_vertex_buffer_object;

		if (this.useVBO) {
			// Updated: Use BufferUtils for IntBuffer creation
			this.vertexBuffers = BufferUtils.createIntBuffer(this.vboCount);
			ARBVertexBufferObject.glGenBuffersARB(this.vertexBuffers);
		}
	}

	public void draw() {
		if (!this.isDrawing) {
			throw new IllegalStateException("Not tesselating!");
		} else {
			this.isDrawing = false;
			if (this.vertexCount > 0) {
				this.intBuffer.clear();
				this.intBuffer.put(this.rawBuffer, 0, this.rawBufferIndex);
				this.byteBuffer.position(0);
				this.byteBuffer.limit(this.rawBufferIndex * 4);
				if (this.useVBO) {
					this.vboIndex = (this.vboIndex + 1) % this.vboCount;
					ARBVertexBufferObject.glBindBufferARB(34962, this.vertexBuffers.get(this.vboIndex));
					ARBVertexBufferObject.glBufferDataARB(34962, this.byteBuffer, 35040);
				}

				if (this.hasTexture) {
					if (this.useVBO) {
						// Updated: Method signature changed - now uses long for offset
						GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 32, 12L);
					} else {
						this.floatBuffer.position(3);
						// Updated: Method signature changed - removed type parameter for FloatBuffer version
						GL11.glTexCoordPointer(2, 32, this.floatBuffer);
					}

					GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
				}

				if (this.hasColor) {
					if (this.useVBO) {
						// Updated: Method signature changed - now uses long for offset
						GL11.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 32, 20L);
					} else {
						this.byteBuffer.position(20);
						// Updated: Method signature changed - proper parameters for ByteBuffer version
						GL11.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 32, this.byteBuffer);
					}

					GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
				}

				if (this.hasNormals) {
					if (this.useVBO) {
						// Updated: Method signature changed - now uses long for offset
						GL11.glNormalPointer(GL11.GL_BYTE, 32, 24L);
					} else {
						this.byteBuffer.position(24);
						// Updated: Method signature changed
						GL11.glNormalPointer(GL11.GL_BYTE, 32, this.byteBuffer);
					}

					GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
				}

				if (this.useVBO) {
					GL11.glVertexPointer(3, GL11.GL_FLOAT, 32, 0L);
				} else {
					this.floatBuffer.position(0);
					GL11.glVertexPointer(3, 32, this.floatBuffer);
				}

				GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
				if (this.drawMode == 7 && convertQuadsToTriangles) {
					GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, this.vertexCount);
				} else {
					GL11.glDrawArrays(this.drawMode, 0, this.vertexCount);
				}

				GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
				if (this.hasTexture) {
					GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
				}

				if (this.hasColor) {
					GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
				}

				if (this.hasNormals) {
					GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
				}
			}

			this.reset();
		}
	}

	private void reset() {
		this.vertexCount = 0;
		this.byteBuffer.clear();
		this.rawBufferIndex = 0;
		this.addedVertices = 0;
	}

	public void startDrawingQuads() {
		this.startDrawing(7);
	}

	public void startDrawing(int i) {
		if (this.isDrawing) {
			throw new IllegalStateException("Already tesselating!");
		} else {
			this.isDrawing = true;
			this.reset();
			this.drawMode = i;
			this.hasNormals = false;
			this.hasColor = false;
			this.hasTexture = false;
			this.isColorDisabled = false;
		}
	}

	public void setTextureUV(double d, double e) {
		this.hasTexture = true;
		this.textureU = d;
		this.textureV = e;
	}

	public void setColorOpaque_F(float f, float g, float h) {
		this.setColorOpaque((int)(f * 255.0F), (int)(g * 255.0F), (int)(h * 255.0F));
	}

	public void setColorRGBA_F(float f, float g, float h, float i) {
		this.setColorRGBA((int)(f * 255.0F), (int)(g * 255.0F), (int)(h * 255.0F), (int)(i * 255.0F));
	}

	public void setColorOpaque(int i, int j, int k) {
		this.setColorRGBA(i, j, k, 255);
	}

	public void setColorRGBA(int i, int j, int k, int l) {
		if (!this.isColorDisabled) {
			if (i > 255) {
				i = 255;
			}

			if (j > 255) {
				j = 255;
			}

			if (k > 255) {
				k = 255;
			}

			if (l > 255) {
				l = 255;
			}

			if (i < 0) {
				i = 0;
			}

			if (j < 0) {
				j = 0;
			}

			if (k < 0) {
				k = 0;
			}

			if (l < 0) {
				l = 0;
			}

			this.hasColor = true;
			if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
				this.color = l << 24 | k << 16 | j << 8 | i;
			} else {
				this.color = i << 24 | j << 16 | k << 8 | l;
			}

		}
	}

	public void addVertexWithUV(double d, double e, double f, double g, double h) {
		this.setTextureUV(g, h);
		this.addVertex(d, e, f);
	}

	public void addVertex(double d, double e, double f) {
		++this.addedVertices;
		if (this.drawMode == 7 && convertQuadsToTriangles && this.addedVertices % 4 == 0) {
			for(int var7 = 0; var7 < 2; ++var7) {
				int var8 = 8 * (3 - var7);
				if (this.hasTexture) {
					this.rawBuffer[this.rawBufferIndex + 3] = this.rawBuffer[this.rawBufferIndex - var8 + 3];
					this.rawBuffer[this.rawBufferIndex + 4] = this.rawBuffer[this.rawBufferIndex - var8 + 4];
				}

				if (this.hasColor) {
					this.rawBuffer[this.rawBufferIndex + 5] = this.rawBuffer[this.rawBufferIndex - var8 + 5];
				}

				this.rawBuffer[this.rawBufferIndex + 0] = this.rawBuffer[this.rawBufferIndex - var8 + 0];
				this.rawBuffer[this.rawBufferIndex + 1] = this.rawBuffer[this.rawBufferIndex - var8 + 1];
				this.rawBuffer[this.rawBufferIndex + 2] = this.rawBuffer[this.rawBufferIndex - var8 + 2];
				++this.vertexCount;
				this.rawBufferIndex += 8;
			}
		}

		if (this.hasTexture) {
			this.rawBuffer[this.rawBufferIndex + 3] = Float.floatToRawIntBits((float)this.textureU);
			this.rawBuffer[this.rawBufferIndex + 4] = Float.floatToRawIntBits((float)this.textureV);
		}

		if (this.hasColor) {
			this.rawBuffer[this.rawBufferIndex + 5] = this.color;
		}

		if (this.hasNormals) {
			this.rawBuffer[this.rawBufferIndex + 6] = this.normal;
		}

		this.rawBuffer[this.rawBufferIndex + 0] = Float.floatToRawIntBits((float)(d + this.xOffset));
		this.rawBuffer[this.rawBufferIndex + 1] = Float.floatToRawIntBits((float)(e + this.yOffset));
		this.rawBuffer[this.rawBufferIndex + 2] = Float.floatToRawIntBits((float)(f + this.zOffset));
		this.rawBufferIndex += 8;
		++this.vertexCount;
		if (this.vertexCount % 4 == 0 && this.rawBufferIndex >= this.bufferSize - 32) {
			this.draw();
			this.isDrawing = true;
		}

	}

	public void setColorOpaque_I(int i) {
		int var2 = i >> 16 & 255;
		int var3 = i >> 8 & 255;
		int var4 = i & 255;
		this.setColorOpaque(var2, var3, var4);
	}

	public void setColorRGBA_I(int i, int j) {
		int var3 = i >> 16 & 255;
		int var4 = i >> 8 & 255;
		int var5 = i & 255;
		this.setColorRGBA(var3, var4, var5, j);
	}

	public void disableColor() {
		this.isColorDisabled = true;
	}

	public void setNormal(float f, float g, float h) {
		if (!this.isDrawing) {
			System.out.println("But..");
		}

		this.hasNormals = true;
		byte var4 = (byte)((int)(f * 128.0F));
		byte var5 = (byte)((int)(g * 127.0F));
		byte var6 = (byte)((int)(h * 127.0F));
		this.normal = var4 | var5 << 8 | var6 << 16;
	}

	public void setTranslationD(double d, double e, double f) {
		this.xOffset = d;
		this.yOffset = e;
		this.zOffset = f;
	}

	public void setTranslationF(float f, float g, float h) {
		this.xOffset += (double)f;
		this.yOffset += (double)g;
		this.zOffset += (double)h;
	}
}
