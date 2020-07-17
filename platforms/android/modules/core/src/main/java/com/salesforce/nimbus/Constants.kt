package com.salesforce.nimbus

import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.JsonConfiguration

const val NIMBUS_BRIDGE = "__nimbus"
const val NIMBUS_PLUGINS = "plugins"
const val NIMBUS_CLASS_DISCRIMINATOR = "__type"
@UnstableDefault
val NIMBUS_JSON_CONFIGURATION = JsonConfiguration(classDiscriminator = NIMBUS_CLASS_DISCRIMINATOR)