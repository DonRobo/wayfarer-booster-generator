package com.wayfarer.boosterupgrade.util.jooq

import org.jooq.Field
import org.jooq.Record
import org.jooq.RecordMapper

abstract class MtgRecordMapper<E : Any?> : RecordMapper<Record, E>, (Record) -> E {
    abstract val fields: List<Field<out Any>>

    final override fun map(record: Record): E {
        return mapData(record)
    }

    abstract fun mapData(r: Record): E

    override fun invoke(r: Record): E = map(r)
}
