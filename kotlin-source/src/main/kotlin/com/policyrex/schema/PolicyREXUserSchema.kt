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
object PolicyREXUserSchema

/**
 * A InsuranceState schema.
 */
object PolicyREXUserSchemaObject : MappedSchema(
        schemaFamily = PolicyREXUserSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentClaim::class.java)) {
    @Entity
    @Table(name = "policyrex_user_states")
    class PersistentClaim(
            @Column(name = "user_node")
            var user_node: String,

            @Column(name = "policyrex_node")
            var policyrex_node: String,

            @Column(name = "first_name")
            var first_name: String,

            @Column(name = "last_name")
            var last_name: String,

            @Column(name = "user_name")
            var user_name: String,

            @Column(name= "id_card")
            var id_card: String,

            @Column(name = "email")
            var email: String,

            @Column(name= "phone")
            var phone: String,

            @Column(name= "address")
            var address: String,

            @Column(name= "status")
            var status: Int,

            @Column(name = "user_id")
            var user_id: UUID

    ) : PersistentState() {
        constructor(): this("", "", "", "", "","","","","", 0, UUID.randomUUID())
    }
}