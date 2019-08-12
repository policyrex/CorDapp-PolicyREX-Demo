package com.policyrex.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for InsuranceState.
 */
object WalletsTransactionsSchema

/**
 * A InsuranceState schema.
 */
object WalletsTransactionsSchemaObject : MappedSchema(
        schemaFamily = UserSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentClaim::class.java)) {
    @Entity
    @Table(name = "wallets_transactions_states")
    class PersistentClaim(
            @Column(name = "user_node")
            var userNode: String,

            @Column(name = "policyrex_node")
            var policyrexNode: String,

            @Column(name = "sender_id")
            var senderId: String,

            @Column(name = "receive_id")
            var receiveId: String,

            @Column(name = "currency")
            var currency: String,

            @Column(name= "value")
            var value: Int,

            @Column(name = "linear_id")
            var linearID: UUID

    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", "", "", "",0, UUID.randomUUID())
    }
}