package me.lpk.mapping.remap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappedMember;
import me.lpk.util.AccessHelper;
import me.lpk.util.ParentUtils;

public class MappingRenamer {
	private static final Set<String> whitelist = new HashSet<String>();

	/**
	 * Updates the information of the given map of MappedClasses according to
	 * the mapping standards given by the MappingMode.
	 * 
	 * @param mappings
	 * @param mode
	 * @return
	 */
	public static Map<String, MappedClass> remapClasses(Map<String, MappedClass> mappings, MappingMode mode) {
		for (MappedClass mc : mappings.values()) {
			mc.setNewName(mode.getClassName(mc.getNode()));
			for (MappedMember mm : mc.getFields()) {
				mm.setNewName(mode.getFieldName(mm.getFieldNode()));
			}
			for (MappedMember mm : mc.getMethods()) {
				MethodNode mn = mm.getMethodNode();
				updateStrings(mn, mappings);
				if (keepName(mm)) {
					// Add more checks in keepName?
					continue;
				}
				MappedMember parentMember = ParentUtils.findMethodOverride(mm);
				// Check and see if theres a parent member to pull names from.
				if (parentMember == null || parentMember.equals(mm)) {
					// No parent found. Not currently renamed.
					if (ParentUtils.callsSuper(mm.getMethodNode())) {
						// Don't rename the method, but mark it as if we did.
						// Parent can't be found but it DOES call a parent.
						mm.setRenamed(true);
					} else {
						// Rename the method.
						mm.setNewName(mode.getMethodName(mm.getMethodNode()));
					}
				} else {
					// Parent found
					// Rename and override current entry.
					mm.setNewName(parentMember.getNewName());
				}
				// methods.put(index, mm);
			}
		}
		return mappings;
	}

	private static void updateStrings(MethodNode mn, Map<String, MappedClass> mappings) {
		
	}

	public static boolean keepName(MappedMember mm) {
		// Main class
		if (mm.getDesc().equals("([Ljava/lang/String;)V") && mm.getOriginalName().equals("main")) {
			return true;
		}
		// <init> or <clinit>
		if (mm.getOriginalName().contains("<")) {
			return true;
		}
		// Synthetic
		// Do all natural synthetic names contain "$"?
		// If so TODO: Add "$" name checks.
		if (AccessHelper.isSynthetic(mm.getMethodNode().access) && !AccessHelper.isPublic(mm.getMethodNode().access)
				&& !AccessHelper.isProtected(mm.getMethodNode().access)) {
			return true;
		}
		// A method name that shan't be renamed!
		if (isNameWhitelisted(mm.getOriginalName())) {
			return true;
		}
		return false;
	}

	public static boolean isNameWhitelisted(String name) {
		return whitelist.contains(name);
	}

	static {
		// TODO: Can I just fix the program so this isn't even needed?
		// TODO: Will need to set this up for a lot of basic methods.
		// Should let user add additional names to the list
		// Have the ugly reflection hack optional for lazy retards like myself.
		Collections.addAll(whitelist, "clone","apply", "compareTo", "equals", "add", "hashCode", "name", "getName", "ordinal", "toString", "valueOf", "values", "get", "clear",
				"iterator", "forEach", "read", "put", "size", "run", "hasNext", "compare", "equals", "defineClass", "findClass", "findResource", "getResource",
				"getResourceAsStream", "indexOf", "replace", "getClass", "finalize", "handle", "actionPerformed", "next");
	}
}