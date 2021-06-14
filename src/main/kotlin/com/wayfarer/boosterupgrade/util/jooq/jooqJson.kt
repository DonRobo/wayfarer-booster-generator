package com.wayfarer.boosterupgrade.util.jooq

import org.jooq.Field
import org.jooq.JSONB
import org.jooq.impl.DSL

fun Field<JSONB>.jsonObject(index: Int): Field<JSONB> {
    return DSL.field("{0}->{1}", JSONB::class.java, this, DSL.inline(index))
}

fun Field<JSONB>.jsonText(index: Int): Field<String> {
    return DSL.field("{0}->>{1}", String::class.java, this, DSL.inline(index))
}

fun Field<JSONB>.jsonObject(name: String): Field<JSONB> {
    return DSL.field("{0}->{1}", JSONB::class.java, this, DSL.inline(name))
}

fun Field<JSONB>.jsonText(name: String): Field<String> {
    return DSL.field("{0}->>{1}", String::class.java, this, DSL.inline(name))
}
