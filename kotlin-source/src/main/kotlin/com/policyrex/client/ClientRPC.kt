package com.policyrex.client

import com.policyrex.state.UserState
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateAndRef
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger

fun main(args: Array<String>) {
    ClientRPC().main(args)
}

private class ClientRPC {
    companion object {
        val logger: Logger = loggerFor<ClientRPC>()
        private fun logState(state: StateAndRef<UserState>) = logger.info("{}", state.state.data)
    }

    fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: ClientRPC <node address>" }
        val nodeAddress = NetworkHostAndPort.parse(args[0])
        val client = CordaRPCClient(nodeAddress)

        // Can be amended in the com.policyrex.MainKt file.
        val proxy = client.start("user1", "test").proxy

        // Grab all existing and future Claim states in the vault.
        val (snapshot, updates) = proxy.vaultTrack(UserState::class.java)

        // Log the 'placed' Claim states and listen for new ones.
        snapshot.states.forEach { logState(it) }
        updates.toBlocking().subscribe { update ->
            update.produced.forEach { logState(it) }
        }
    }
}
