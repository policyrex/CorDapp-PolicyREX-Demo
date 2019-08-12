package com.policyrex.state

import com.policyrex.schema.WalletsTransactionsSchemaObject
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState


data class WalletsTransactionsState(
        val userNode: Party,
        val policyREXNode: Party,
        val senderID: String,
        val receiveID: String,
        val currency: String,
        val type: String,
        val value:Int,
        override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {

    override val participants: List<AbstractParty> get() = listOf(userNode, policyREXNode)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is WalletsTransactionsSchemaObject -> WalletsTransactionsSchemaObject.PersistentClaim(
                    this.userNode.name.toString(),
                    this.policyREXNode.name.toString(),
                    this.senderID,
                    this.receiveID,
                    this.currency,
                    this.type,
                    this.value,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(WalletsTransactionsSchemaObject)
}
