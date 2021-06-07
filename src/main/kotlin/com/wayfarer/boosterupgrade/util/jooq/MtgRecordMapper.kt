package com.wayfarer.boosterupgrade.util.jooq

import org.jooq.Field
import org.jooq.Record
import org.jooq.RecordMapper

abstract class MtgRecordMapper<E : Any?> : RecordMapper<Record, E> {
    abstract val fields: List<Field<out Any>>
}
