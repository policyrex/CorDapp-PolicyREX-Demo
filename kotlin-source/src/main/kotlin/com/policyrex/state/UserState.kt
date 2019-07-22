package com.policyrex.state

import com.policyrex.schema.UserSchemaObject
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.util.*

data class UserState(
        val user_node: Party,
        val policyrex_node: Party,
        val first_name: String,
        val last_name: String,
        var user_name: String,
        var id_card: String,
        val email: String,
        val phone: String,
        val address: String,
        var status: Int,
        override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(user_node, policyrex_node)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is UserSchemaObject -> UserSchemaObject.PersistentClaim(
                this.user_node.name.toString(),
                this.policyrex_node.name.toString(),
                this.first_name,
                this.last_name,
                this.user_name,
                this.id_card,
                this.email,
                this.phone,
                this.address,
                this.status,
                this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(UserSchemaObject)
}
