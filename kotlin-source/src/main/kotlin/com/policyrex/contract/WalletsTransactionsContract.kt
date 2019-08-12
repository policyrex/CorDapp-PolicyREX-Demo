package com.policyrex.contract

import com.policyrex.state.WalletsTransactionsState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction


class WalletsTransactionsContract : Contract {
    companion object {
        @JvmStatic
        val TRANSACTION_CONTRACT_ID = "com.policyrex.contract.WalletsTransactionsContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands.Create>()
        requireThat {
            // Generic constraints around the transaction.
            "No inputs should be consumed when issuing an Transactions." using (tx.inputs.isEmpty())
            "Only one output state should be created." using (tx.outputs.size == 1)
            val out = tx.outputsOfType<WalletsTransactionsState>().single()
            "The sender and the Receive cannot be the same entity." using (out.userNode != out.policyREXNode)
            "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))
            "The value must be non-negative." using (out.value > 0)
        }
    }

    interface Commands : CommandData {
        class Create : Commands
    }
}
