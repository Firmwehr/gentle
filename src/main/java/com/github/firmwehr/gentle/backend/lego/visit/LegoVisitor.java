package com.github.firmwehr.gentle.backend.lego.visit;

import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoAdd;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoArgNode;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoCall;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoCmp;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoConst;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoConv;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoCopy;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoDiv;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoJcc;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoJmp;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMovLoad;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMovLoadEx;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMovRegister;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMovStore;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMovStoreEx;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMul;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoNeg;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoNode;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoPerm;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoPhi;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoProj;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoReload;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoRet;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoShl;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoShr;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoShrs;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoSpill;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoSub;

public interface LegoVisitor<T> {

	T defaultReturnValue();

	default T defaultVisit(LegoNode node) {
		return defaultReturnValue();
	}

	default T visit(LegoAdd add) {
		return defaultVisit(add);
	}

	default T visit(LegoCall call) {
		return defaultVisit(call);
	}

	default T visit(LegoCmp cmp) {
		return defaultVisit(cmp);
	}

	default T visit(LegoConst legoConst) {
		return defaultVisit(legoConst);
	}

	default T visit(LegoConv conv) {
		return defaultVisit(conv);
	}

	default T visit(LegoDiv div) {
		return defaultVisit(div);
	}

	default T visit(LegoJcc jcc) {
		return defaultVisit(jcc);
	}

	default T visit(LegoJmp jmp) {
		return defaultVisit(jmp);
	}

	default T visit(LegoMovLoad movLoad) {
		return defaultVisit(movLoad);
	}

	default T visit(LegoMovLoadEx movLoadEx) {
		return defaultVisit(movLoadEx);
	}

	default T visit(LegoMovRegister movRegister) {
		return defaultVisit(movRegister);
	}

	default T visit(LegoMovStore movStore) {
		return defaultVisit(movStore);
	}

	default T visit(LegoMovStoreEx movStoreEx) {
		return defaultVisit(movStoreEx);
	}

	default T visit(LegoMul mul) {
		return defaultVisit(mul);
	}

	default T visit(LegoNeg neg) {
		return defaultVisit(neg);
	}

	default T visit(LegoPhi phi) {
		return defaultVisit(phi);
	}

	default T visit(LegoRet ret) {
		return defaultVisit(ret);
	}

	default T visit(LegoSub sub) {
		return defaultVisit(sub);
	}

	default T visit(LegoShl shl) {
		return defaultVisit(shl);
	}

	default T visit(LegoShr shr) {
		return defaultVisit(shr);
	}

	default T visit(LegoShrs shrs) {
		return defaultVisit(shrs);
	}

	default T visit(LegoReload reload) {
		return defaultVisit(reload);
	}

	default T visit(LegoCopy copy) {
		return defaultVisit(copy);
	}

	default T visit(LegoSpill spill) {
		return defaultVisit(spill);
	}

	default T visit(LegoPerm perm) {
		return defaultVisit(perm);
	}

	default T visit(LegoProj proj) {
		return defaultVisit(proj);
	}

	default T visit(LegoArgNode argNode) {
		return defaultVisit(argNode);
	}

	default T visit(LegoPlate block) {
		return defaultReturnValue();
	}
}
