package com.grahamlea.forbiddenisland.play.markusshepherd

import java.util.Random

fun <T> choice(elements: List<T?>, random: Random = Random()): T? = if (elements.isEmpty())
    null
else
    elements[random.nextInt(elements.size)]
