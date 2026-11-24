package com.blr19c.falowp.bot.plugins.image.runtime.vips

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
import java.lang.invoke.MethodHandle
import java.nio.file.Path

/**
 * libvips函数绑定，变长参数按当前平台的C调用约定传递
 */
internal class VipsOperations(library: Path) {

    private val symbols = SymbolLookup.libraryLookup(library, Arena.global())
    private val linker = Linker.nativeLinker()

    private fun bind(name: String, result: MemoryLayout?, vararg args: MemoryLayout, fixed: Int = args.size): MethodHandle {
        val descriptor = if (result == null) FunctionDescriptor.ofVoid(*args) else FunctionDescriptor.of(result, *args)
        val symbol = symbols.findOrThrow(name)
        if (fixed == args.size) return linker.downcallHandle(symbol, descriptor)
        return linker.downcallHandle(symbol, descriptor, Linker.Option.firstVariadicArg(fixed))
    }

    private fun operation(name: String, vararg args: MemoryLayout, options: List<MemoryLayout> = emptyList()) =
        bind("vips_$name", JAVA_INT, ADDRESS, ADDRESS, *args, *options.toTypedArray(), ADDRESS, fixed = 2 + args.size)

    val findLoader = bind("vips_foreign_find_load", ADDRESS, ADDRESS)
    val openFile = bind("vips_image_new_from_file", ADDRESS, ADDRESS, ADDRESS, fixed = 1)
    val orientation = bind("vips_image_get_orientation", JAVA_INT, ADDRESS)
    val preview = bind("vips_thumbnail", JAVA_INT, ADDRESS, ADDRESS, JAVA_INT,
        ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, fixed = 3)
    val pixels = bind("vips_image_write_to_memory", ADDRESS, ADDRESS, ADDRESS)
    val fromPixels = bind("vips_image_new_from_memory", ADDRESS, ADDRESS, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT)
    val jpegSave = bind("vips_jpegsave", JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, fixed = 2)
    val open = bind("vips_image_new_from_buffer", ADDRESS, ADDRESS, JAVA_LONG, ADDRESS, ADDRESS, fixed = 3)
    val thumbnail = bind("vips_thumbnail_buffer", JAVA_INT, ADDRESS, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS, fixed = 4)
    val encode = bind("vips_image_write_to_buffer", JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, fixed = 4)
    val width = bind("vips_image_get_width", JAVA_INT, ADDRESS)
    val height = bind("vips_image_get_height", JAVA_INT, ADDRESS)
    val hasAlpha = bind("vips_image_hasalpha", JAVA_INT, ADDRESS)
    val unref = bind("g_object_unref", null, ADDRESS)
    val free = bind("g_free", null, ADDRESS)
    private val errorBuffer = bind("vips_error_buffer", ADDRESS)
    val clearError = bind("vips_error_clear", null)

    val autorot = operation("autorot")
    val crop = operation("extract_area", JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT)
    val resize = operation("resize", JAVA_DOUBLE, options = listOf(ADDRESS, JAVA_DOUBLE, ADDRESS, JAVA_INT))
    val rotate = operation("rotate", JAVA_DOUBLE)
    val rot = operation("rot", JAVA_INT)
    val flip = operation("flip", JAVA_INT)
    val pad = operation("embed", JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT)
    val arrayDouble = bind("vips_array_double_new", ADDRESS, ADDRESS, JAVA_INT)
    val areaUnref = bind("vips_area_unref", null, ADDRESS)
    val flatten = operation("flatten", options = listOf(ADDRESS, ADDRESS))
    val addAlpha = operation("addalpha")
    val colourspace = operation("colourspace", JAVA_INT)
    val invert = operation("invert")
    val linear = operation("linear1", JAVA_DOUBLE, JAVA_DOUBLE)
    val linearBands = operation("linear", ADDRESS, ADDRESS, JAVA_INT)
    val cast = operation("cast", JAVA_INT)
    val threshold = operation("relational_const1", JAVA_INT, JAVA_DOUBLE)
    val blur = operation("gaussblur", JAVA_DOUBLE)
    val sharpen = operation("sharpen")
    val median = operation("rank", JAVA_INT, JAVA_INT, JAVA_INT)
    val extract = operation("extract_band", JAVA_INT, options = listOf(ADDRESS, JAVA_INT))
    val bandjoin = bind("vips_bandjoin2", JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, fixed = 3)
    val overlay = bind("vips_composite2", JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT,
        ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, fixed = 4)
    val join = bind("vips_join", JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, fixed = 4)

    init {
        val init = bind("vips_init", JAVA_INT, ADDRESS)
        Arena.ofConfined().use { checkResult(init.invoke(it.allocateFrom("falowp-bot")) as Int) }
        //输入缓冲区属于单次任务，避免原生缓存将其保留到任务结束之后
        bind("vips_cache_set_max", null, JAVA_INT).invoke(0)
    }

    fun checkResult(status: Int) {
        if (status == 0) return
        val message = (errorBuffer.invoke() as MemorySegment).reinterpret(Long.MAX_VALUE).getString(0)
        clearError.invoke()
        error("libvips: ${message.trim()}")
    }
}
