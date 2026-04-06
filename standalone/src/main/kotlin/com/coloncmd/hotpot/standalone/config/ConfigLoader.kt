package com.coloncmd.hotpot.standalone.config

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import java.nio.file.Path
import kotlin.io.path.readText

object ConfigLoader {
    private val ENV_PATTERN = Regex("""\$\{([A-Z0-9_]+)\}""")

    private val yaml =
        Yaml(
            configuration =
                YamlConfiguration(
                    polymorphismStyle = PolymorphismStyle.Property,
                ),
        )

    fun load(path: Path): HotPotConfig = load(path.readText())

    fun load(
        yamlText: String,
        envLookup: (String) -> String? = System::getenv,
    ): HotPotConfig {
        val substituted =
            ENV_PATTERN.replace(yamlText) { match ->
                val name = match.groupValues[1]
                envLookup(name) ?: error("Required environment variable '$name' is not set")
            }
        return yaml.decodeFromString(HotPotConfig.serializer(), substituted)
    }
}
