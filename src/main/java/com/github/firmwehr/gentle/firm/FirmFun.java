package com.github.firmwehr.gentle.firm;

import firm.Construction;
import firm.Dump;
import firm.Entity;
import firm.Firm;
import firm.Graph;
import firm.MethodType;
import firm.Program;
import firm.nodes.Node;

public class FirmFun {

	public static void main(String[] args) {
		Firm.init("x86_64-linux-gnu", new String[]{"pic=1"});

		MethodType mainMethod = new MethodType(0, 0);
		Entity mainEntity = new Entity(Program.getGlobalType(), "main", mainMethod);

		Graph mainGraph = new Graph(mainEntity, 0);
		Construction construction = new Construction(mainGraph);

		construction.newReturn(construction.getCurrentMem(), new Node[0]);

		construction.finish();

		Dump.dumpGraph(mainGraph, "test");

		Firm.finish();
	}

}
