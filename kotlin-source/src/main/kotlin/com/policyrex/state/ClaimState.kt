package com.policyrex.state

import com.policyrex.schema.ClaimSchemaObject
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState


data class ClaimState(
        val applicantNode: Party,
        val insurerNode: Party,
        val userID: String,
        val userName: String,
        val address: String,
        val value:Int,
        val approvedAmount:Int,
        var insuranceStatus: String,
        override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {

    override val participants: List<AbstractParty> get() = listOf(insurerNode,applicantNode)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is ClaimSchemaObject -> ClaimSchemaObject.PersistentClaim(
                    this.applicantNode.name.toString(),
                    this.insurerNode.name.toString(),
                    this.userID,
                    this.userName,
                    this.address,
                    this.value,
                    this.approvedAmount,
                    this.insuranceStatus,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(ClaimSchemaObject)
}
