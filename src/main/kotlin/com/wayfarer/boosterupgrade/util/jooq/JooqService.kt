package com.wayfarer.boosterupgrade.util.jooq

import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.ResultQuery


fun DSLContext.executeDeferConstraints() {
    execute("SET CONSTRAINTS ALL DEFERRED")
}


fun <R : Record> ResultQuery<R>.fetchSingleOrNull() = this.fetch().let {
    if (it.size == 1) {
        it[0]
    } else {
        null
    }
}
