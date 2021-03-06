package com.github.firmwehr.gentle.backend.ir.visit;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaAdd;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaAnd;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaArgNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaCall;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaCmp;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaConst;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaConv;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaDiv;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaJcc;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaJmp;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaLea;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovLoad;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovLoadEx;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovRegister;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovStore;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovStoreEx;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMul;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNeg;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaPhi;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaRet;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaSet;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaShl;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaShr;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaShrs;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaSub;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaXor;

public interface IkeaVisitor<T> {

	T defaultReturnValue();

	default T defaultVisit(IkeaNode node) {
		return defaultReturnValue();
	}

	default T visit(IkeaAdd add) {
		return defaultVisit(add);
	}

	default T visit(IkeaAnd and) {
		return defaultVisit(and);
	}

	default T visit(IkeaArgNode argNode) {
		return defaultVisit(argNode);
	}

	default T visit(IkeaCall call) {
		return defaultVisit(call);
	}

	default T visit(IkeaCmp cmp) {
		return defaultVisit(cmp);
	}

	default T visit(IkeaConst ikeaConst) {
		return defaultVisit(ikeaConst);
	}

	default T visit(IkeaConv conv) {
		return defaultVisit(conv);
	}

	default T visit(IkeaDiv div) {
		return defaultVisit(div);
	}

	default T visit(IkeaJcc jcc) {
		return defaultVisit(jcc);
	}

	default T visit(IkeaJmp jmp) {
		return defaultVisit(jmp);
	}

	default T visit(IkeaLea lea) {
		return defaultVisit(lea);
	}

	default T visit(IkeaMovLoad movLoad) {
		return defaultVisit(movLoad);
	}

	default T visit(IkeaMovLoadEx movLoadEx) {
		return defaultVisit(movLoadEx);
	}

	default T visit(IkeaMovRegister movRegister) {
		return defaultVisit(movRegister);
	}

	default T visit(IkeaMovStore movStore) {
		return defaultVisit(movStore);
	}

	default T visit(IkeaMovStoreEx movStoreEx) {
		return defaultVisit(movStoreEx);
	}

	default T visit(IkeaMul mul) {
		return defaultVisit(mul);
	}

	default T visit(IkeaNeg neg) {
		return defaultVisit(neg);
	}

	default T visit(IkeaPhi phi) {
		return defaultVisit(phi);
	}

	default T visit(IkeaRet ret) {
		return defaultVisit(ret);
	}

	default T visit(IkeaSet set) {
		return defaultVisit(set);
	}

	default T visit(IkeaSub sub) {
		return defaultVisit(sub);
	}

	default T visit(IkeaShl shl) {
		return defaultVisit(shl);
	}

	default T visit(IkeaShr shr) {
		return defaultVisit(shr);
	}

	default T visit(IkeaShrs shrs) {
		return defaultVisit(shrs);
	}

	default T visit(IkeaXor xor) {
		return defaultVisit(xor);
	}

	default T visit(IkeaBløck block) {
		return defaultReturnValue();
	}
}
