package com.policyrex.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


object ClaimSchema

object ClaimSchemaObject : MappedSchema(
        schemaFamily = ClaimSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentClaim::class.java)) {
    @Entity
    @Table(name = "claim_states")
    class PersistentClaim(
            @Column(name = "applicant")
            var applicant: String,

            @Column(name = "insurer")
            var insurer: String,

            @Column(name = "userId")
            var userId: String,

            @Column(name = "userName")
            var userName: String,

            @Column(name = "address")
            var address: String,

            @Column(name = "value")
            var value: Int,

            @Column(name = "approvedAmount")
            var approvedAmount: Int,

            @Column(name = "insuranceStatus")
            var insuranceStatus: String,

            @Column(name = "claimID")
            var claimID: UUID

    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", "","","",0,0, "", UUID.randomUUID())
    }
}