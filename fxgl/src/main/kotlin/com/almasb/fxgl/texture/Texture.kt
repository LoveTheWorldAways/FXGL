/*
 * FXGL - JavaFX Game Library. The MIT License (MIT).
 * Copyright (c) AlmasB (almaslvl@gmail.com).
 * See LICENSE for details.
 */

package com.almasb.fxgl.texture


import com.almasb.fxgl.core.Disposable
import com.almasb.fxgl.core.concurrent.Async
import com.almasb.fxgl.core.math.FXGLMath
import javafx.geometry.HorizontalDirection
import javafx.geometry.Rectangle2D
import javafx.geometry.VerticalDirection
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.SnapshotParameters
import javafx.scene.effect.BlendMode
import javafx.scene.image.*
import javafx.scene.paint.Color
import javafx.util.Duration

import java.lang.Math.min

/**
 * Represents a 2D image which can be set as view for an entity.
 * The size ratio and viewport can be modified as necessary.
 *
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 * @apiNote This is essentially a wrapper around [javafx.scene.image.ImageView]
 */
open class Texture
/**
 * Constructs new texture from given image.
 *
 * @param image the JavaFX image data
 */
(image: Image) : ImageView(image), Disposable {

    /**
     * Converts the texture to animated texture using
     * the whole texture as a single animation channel.
     * Must be in 1 row.
     *
     * @param frames   number of frames in sprite sheet
     * @param duration overall duration (for all frames) of the animation
     * @return new AnimatedTexture
     */
    fun toAnimatedTexture(frames: Int, duration: Duration): AnimatedTexture {
        return toAnimatedTexture(AnimationChannel(
                image,
                frames, image.width.toInt() / frames, image.height.toInt(),
                duration, 0, frames - 1)
        )
    }

    /**
     * Converts the texture to animated texture.
     *
     * @param defaultChannel the default channel
     * @return new AnimatedTexture
     */
    fun toAnimatedTexture(defaultChannel: AnimationChannel): AnimatedTexture {
        return AnimatedTexture(defaultChannel)
    }

    /**
     * Call this to create a new texture if you are
     * planning to use the same image as graphics
     * for multiple entities.
     * This is required because same Node can only have 1 parent.
     *
     *
     * Do NOT invoke on instances of StaticAnimatedTexture or
     * AnimatedTexture, use [.toAnimatedTexture]
     * or [.toAnimatedTexture] instead.
     *
     * @return new Texture with same image
     */
    fun copy(): Texture {
        return Texture(image)
    }

    /**
     * Given a rectangular area, produces a sub-texture of
     * this texture.
     *
     *
     * Rectangle cannot cover area outside of the original texture
     * image.
     *
     * @param area area of the original texture that represents sub-texture
     * @return sub-texture
     */
    fun subTexture(area: Rectangle2D): Texture {
        val minX = area.minX.toInt()
        val minY = area.minY.toInt()
        val maxX = area.maxX.toInt()
        val maxY = area.maxY.toInt()

        if (minX < 0)
            throw IllegalArgumentException("minX value of sub-texture cannot be negative")
        if (minY < 0)
            throw IllegalArgumentException("minY value of sub-texture cannot be negative")
        if (maxX > image.width)
            throw IllegalArgumentException("maxX value of sub-texture cannot be greater than image width")
        if (maxY > image.height)
            throw IllegalArgumentException("maxY value of sub-texture cannot be greater than image height")

        val pixelReader = image.pixelReader
        val image = WritableImage(maxX - minX, maxY - minY)
        val pixelWriter = image.pixelWriter

        for (y in minY until maxY) {
            for (x in minX until maxX) {
                val color = pixelReader.getColor(x, y)
                pixelWriter.setColor(x - minX, y - minY, color)
            }
        }

        return Texture(image)
    }

    /**
     * Generates a new texture which combines this and given texture.
     * The given texture is appended based on the direction provided.
     *
     * @param other the texture to append to this one
     * @param direction the direction to append from
     * @return new combined texture
     */
    fun superTexture(other: Texture, direction: HorizontalDirection): Texture {
        val leftImage: Image
        val rightImage: Image

        if (direction == HorizontalDirection.LEFT) {
            leftImage = other.image
            rightImage = this.image
        } else {
            leftImage = this.image
            rightImage = other.image
        }

        val width = (leftImage.width + rightImage.width).toInt()
        val height = Math.max(leftImage.height, rightImage.height).toInt()

        val leftReader = leftImage.pixelReader
        val rightReader = rightImage.pixelReader
        val image = WritableImage(width, height)
        val pixelWriter = image.pixelWriter

        for (y in 0 until height) {
            for (x in 0 until width) {
                val color: Color
                if (x < leftImage.width) {
                    if (y < leftImage.height) {
                        color = leftReader.getColor(x, y)
                    } else {
                        color = Color.TRANSPARENT
                    }
                } else {
                    if (y < rightImage.height) {
                        color = rightReader.getColor(x - leftImage.width.toInt(), y)
                    } else {
                        color = Color.TRANSPARENT
                    }
                }

                pixelWriter.setColor(x, y, color)
            }
        }

        return Texture(image)
    }

    /**
     * Generates a new texture which combines this and given texture.
     * The given texture is appended based on the direction provided.
     *
     * @param other the texture to append to this one
     * @param direction the direction to append from
     * @return new combined texture
     */
    fun superTexture(other: Texture, direction: VerticalDirection): Texture {
        val topImage: Image
        val bottomImage: Image

        if (direction == VerticalDirection.DOWN) {
            topImage = this.image
            bottomImage = other.image
        } else {
            topImage = other.image
            bottomImage = this.image
        }

        val width = Math.max(topImage.width, bottomImage.width).toInt()
        val height = (topImage.height + bottomImage.height).toInt()

        val topReader = topImage.pixelReader
        val bottomReader = bottomImage.pixelReader
        val image = WritableImage(width, height)
        val pixelWriter = image.pixelWriter

        for (y in 0 until height) {
            for (x in 0 until width) {
                val color: Color
                if (y < topImage.height) {
                    if (x < topImage.width) {
                        color = topReader.getColor(x, y)
                    } else {
                        color = Color.TRANSPARENT
                    }
                } else {
                    if (x < bottomImage.width) {
                        color = bottomReader.getColor(x, y - topImage.height.toInt())
                    } else {
                        color = Color.TRANSPARENT
                    }
                }

                pixelWriter.setColor(x, y, color)
            }
        }

        return Texture(image)
    }

    /**
     * @return grayscale version of the texture
     */
    fun toGrayscale(): Texture {
        val w = image.width.toInt()
        val h = image.height.toInt()

        val reader = image.pixelReader
        val image = WritableImage(w, h)
        val writer = image.pixelWriter

        for (y in 0 until h) {
            for (x in 0 until w) {
                writer.setColor(x, y, reader.getColor(x, y).grayscale())
            }
        }

        return Texture(image)
    }

    /**
     * Discoloring is done via setting each pixel to white but
     * preserving opacity (alpha channel).
     *
     * @return texture with image discolored
     */
    fun discolor(): Texture {
        val w = image.width.toInt()
        val h = image.height.toInt()

        val reader = image.pixelReader
        val image = WritableImage(w, h)
        val writer = image.pixelWriter

        for (y in 0 until h) {
            for (x in 0 until w) {
                val opacity = reader.getColor(x, y).opacity
                writer.setColor(x, y, Color.color(1.0, 1.0, 1.0, opacity))
            }
        }

        return Texture(image)
    }

    /**
     * Multiplies this texture's pixel color with given color.
     *
     * @param color to use
     * @return new colorized texture
     */
    fun multiplyColor(color: Color): Texture {
        val w = image.width.toInt()
        val h = image.height.toInt()

        val reader = image.pixelReader
        val coloredImage = WritableImage(w, h)
        val writer = coloredImage.pixelWriter

        for (y in 0 until h) {
            for (x in 0 until w) {

                var c = reader.getColor(x, y)
                c = Color.color(
                        c.red * color.red,
                        c.green * color.green,
                        c.blue * color.blue,
                        c.opacity * color.opacity
                )

                writer.setColor(x, y, c)
            }
        }

        return Texture(coloredImage)
    }

    /**
     * Colorizes this texture's pixels with given color.
     *
     * @param color to use
     * @return new colorized texture
     */
    fun toColor(color: Color): Texture {
        val discolored = discolor()
        val colored = discolored.multiplyColor(color)
        discolored.dispose()

        return colored
    }

    /**
     * @param color transparent color
     * @return new texture whose pixels with given color are set to transparent
     */
    fun transparentColor(color: Color): Texture {
        val newImage = image.map {
            val newColor = if (it.color == color) {
                Color.TRANSPARENT
            } else {
                it.color
            }

            it.copy(newColor)
        }

        return Texture(newImage)
    }

    /**
     * @param background the node with which to blend
     * @param blendMode blend mode
     * @return new texture using a blended image of this texture
     */
    fun blend(background: Node, blendMode: BlendMode): Texture {
        if (background.parent != null) {
            throw IllegalArgumentException("The blend background must not be attached to a parent.")
        }

        val w = image.width.toInt()
        val h = image.height.toInt()

        val bgImage = WritableImage(w, h)

        Async.startFX {
            val params = SnapshotParameters()
            params.fill = Color.TRANSPARENT

            background.snapshot(params, bgImage)
        }.await()

        return Texture(bgImage.map(image, blendMode.operation()))
    }

    /**
     * Set texture data by copying it from other texture.
     *
     * @param other the texture to copy from
     */
    fun set(other: Texture) {
        fitWidth = other.fitWidth
        fitHeight = other.fitHeight
        image = other.image
    }

    override fun dispose() {
        image = null
    }

    override fun toString(): String {
        return "Texture [fitWidth=$fitWidth, fitHeight=$fitHeight]"
    }
}
