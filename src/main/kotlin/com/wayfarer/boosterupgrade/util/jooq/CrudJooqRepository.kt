package com.wayfarer.boosterupgrade.util.jooq

import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Table
import org.jooq.UpdatableRecord

abstract class CrudJooqRepository<R : UpdatableRecord<*>>(val ctx: DSLContext, val table: Table<R>) {

    private val Table<R>.primaryKeyChecked get() = primaryKey ?: error("No primary key set for $table")

    open fun update(record: R): Int {
        return ctx.executeUpdate(record)
    }

    open fun insertAll(records: Collection<R>): Int {
        return ctx.insertAll(records)
    }

    open fun upsertAll(records: Collection<R>): Int {
        return ctx.batch(
            records.map { record ->
                ctx.insertInto(table)
                    .set(record)
                    .onConflict(table.primaryKeyChecked.fields).doUpdate()
                    .set(record)
            }
        ).execute().sum()
    }

    open fun deleteAll(): Int {
        return ctx.deleteFrom(table)
            .execute()
    }

    open fun insert(record: R): R {
        return ctx.insertInto(table)
            .set(record)
            .returning()
            .fetchOne() ?: error("Insert failed somehow")
    }

    open fun count(): Int {
        return ctx.fetchCount(table)
    }

    open fun upsert(record: R): R {
        return ctx.insertInto(table)
            .set(record)
            .onConflict(table.primaryKeyChecked.fields).doUpdate()
            .set(record)
            .returning()
            .fetchOne() ?: error("Upsert failed somehow")
    }

    open fun selectAll(): List<R> {
        return ctx.fetch(table).toList()
    }

    open fun newRecord(): R {
        return ctx.newRecord(table)
    }

    @Suppress("UNCHECKED_CAST")
    open fun <T> findByIds(keys: Collection<T>): Map<T, R> {
        require(table.primaryKeyChecked.fields.size == 1)

        return ctx.selectFrom(table)
            .where(table.primaryKeyChecked.fields.single().`in`(keys))
            .fetch()
            .intoMap {
                it[table.primaryKeyChecked.fields.single()] as T
            }
    }

    @Suppress("UNCHECKED_CAST")
    open fun <T> findById(id: T): R {
        require(table.primaryKeyChecked.fields.size == 1)

        val pk: Field<T> = table.primaryKeyChecked.fields.single() as Field<T>

        return ctx.selectFrom(table)
            .where(pk.eq(id))
            .fetch()
            .single()
    }

    @Suppress("UNCHECKED_CAST")
    open fun <T> findByIdOptional(id: T): R? {
        require(table.primaryKeyChecked.fields.size == 1)

        val pk: Field<T> = table.primaryKeyChecked.fields.single() as Field<T>

        return ctx.selectFrom(table)
            .where(pk.eq(id))
            .fetch()
            .singleOrNull()
    }

    @Suppress("UNCHECKED_CAST")
    open fun <T> deleteById(id: T): Int {
        require(table.primaryKeyChecked.fields.size == 1)
        val pk: Field<T> = table.primaryKeyChecked.fields.single() as Field<T>

        return ctx.deleteFrom(table)
            .where(pk.eq(id))
            .execute()
    }

    open fun updateAll(records: List<R>): Int {
        return ctx.batchUpdate(records).execute().sum()
    }
}

fun DSLContext.insertAll(records: Collection<UpdatableRecord<*>>): Int {
    return batchInsert(records).execute().sum()
}
