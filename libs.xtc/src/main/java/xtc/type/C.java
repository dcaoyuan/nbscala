/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2007-2008 Robert Grimm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */
package xtc.type;

import java.math.BigInteger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import xtc.Constants;
import xtc.Limits;

import xtc.tree.Attribute;

import xtc.util.Utilities;

/**
 * Common type operations for the C language.
 *
 * @author Robert Grimm
 * @version $Revision: 1.43 $
 */
public class C {

  /** Create a new instance. */
  public C() { /* Nothing to do. */ }

  /**
   * The canonical implicit int type.  In K&R C, a missing type
   * specifier is treated as an int.  We preserve knowledge about this
   * lack of type specifier through this type, which has a {@link
   * Constants#ATT_IMPLICIT} attribute.
   */
  public static final IntegerT IMPLICIT = new IntegerT(NumberT.Kind.INT);

  static {
    IMPLICIT.addAttribute(Constants.ATT_IMPLICIT);
    IMPLICIT.seal();
  }

  /** The integer kind of the sizeof type. */
  protected static final NumberT.Kind KIND_SIZEOF =
    IntegerT.fromRank(Limits.SIZEOF_RANK, false);

  /** The canonical sizeof type. */
  public static final IntegerT SIZEOF = new IntegerT(KIND_SIZEOF);

  static {
    SIZEOF.seal();
  }

  /** The integer kind of the pointer difference type. */
  protected static final NumberT.Kind KIND_PTR_DIFF =
    IntegerT.fromRank(Limits.PTRDIFF_RANK, true);

  /** The canonical pointer difference type. */
  public static final IntegerT PTR_DIFF = new IntegerT(KIND_PTR_DIFF);

  static {
    PTR_DIFF.seal();
  }

  /** The number kind of the wchar_t type. */
  protected static final NumberT.Kind KIND_WCHAR =
    IntegerT.fromRank(Limits.WCHAR_RANK, Limits.IS_WCHAR_SIGNED);

  /** The canonical wide char type. */
  public static final IntegerT WCHAR = new IntegerT(KIND_WCHAR);

  static {
    WCHAR.seal();
  }

  // =========================================================================

  /**
   * Determine whether the specified type is a char.
   *
   * @param type The type.
   * @return <code>true</code> if the type is a char.
   */
  public boolean isChar(Type type) {
    if (type.hasEnum()) return false;

    type = type.resolve();

    if (type.isInteger()) {
      switch (type.toInteger().getKind()) {
      case CHAR:
      case S_CHAR:
      case U_CHAR:
        return true;
      default:
        return false;
      }
    } else {
      return false;
    }
  }

  /**
   * Determine whether the specified type is a wide char.
   *
   * @param type The type.
   * @return <code>true</code> if the type is a wide char.
   */
  public boolean isWideChar(Type type) {
    if (type.hasEnum()) return false;

    type = type.resolve();

    if (type.isInteger()) {
      return NumberT.equal(KIND_WCHAR, type.toInteger().getKind());
    } else {
      return false;
    }
  }

  /**
   * Determine whether the specified type is a string.
   *
   * @param type The type.
   * @return <code>true</code> if the type is a string.
   */
  public boolean isString(Type type) {
    type = type.resolve();

    return type.isArray() && isChar(type.toArray().getType());
  }

  /**
   * Determine whether the specified type is a wide string.
   *
   * @param type The type.
   * @return <code>true</code> if the type is a wide string.
   */
  public boolean isWideString(Type type) {
    type = type.resolve();

    return type.isArray() && isWideChar(type.toArray().getType());
  }

  /**
   * Determine whether the specified type is a string literal.
   *
   * @param type The type.
   * @return <code>true</code> if the type is a string literal.
   */
  public boolean isStringLiteral(Type type) {
    return (isString(type) || isWideString(type)) && type.hasConstant();
  }

  // =========================================================================

  /**
   * Determine whether the specified type is integral.
   *
   * @param type The type.
   * @return <code>true</code> if the specified type is integral.
   */
  public boolean isIntegral(Type type) {
    switch (type.tag()) {
    case BOOLEAN:
    case INTEGER:
      return true;
    default:
      return false;
    }
  }

  /**
   * Determine whether the specified type is real.
   *
   * @param type The type.
   * @return <code>true</code> if the specified type is real.
   */
  public boolean isReal(Type type) {
    switch (type.tag()) {
    case BOOLEAN:
    case INTEGER:
      return true;
    case FLOAT:
      switch (type.resolve().toFloat().getKind()) {
      case FLOAT_COMPLEX:
      case DOUBLE_COMPLEX:
      case LONG_DOUBLE_COMPLEX:
        return false;
      default:
        return true;
      }
    default:
      return false;
    }
  }

  /**
   * Determine whether the specified type is arithmetic.
   *
   * @param type The type.
   * @return <code>true</code> if the specified type is arithmetic.
   */
  public boolean isArithmetic(Type type) {
    switch (type.tag()) {
    case BOOLEAN:
    case INTEGER:
    case FLOAT:
      return true;
    default:
      return false;
    }
  }

  /**
   * Determine whether the specified type is scalar.
   *
   * @param type The type.
   * @return <code>true</code> if the specified type is scalar.
   */
  public boolean isScalar(Type type) {
    switch (type.tag()) {
    case BOOLEAN:
    case INTEGER:
    case FLOAT:
    case POINTER:
      return true;
    default:
      return false;
    }
  }

  /**
   * Determine whether the specified type is incomplete.
   *
   * <p />Per C99 6.2.5, a type is incomplete if it does not contain
   * sufficient information for determining its size.  However, per
   * C99 6.7.2.1, a struct type with more than one named member may
   * have an incomplete type as its last member, while still being
   * considered complete.
   *
   * @param type The type.
   * @return <code>true</code> if the type is incomplete.
   */
  public boolean isIncomplete(Type type) {
    // Handle alias and enum types first since they are wrapped and do
    // not have a tag.
    while (type.isWrapped()) {
      switch (type.wtag()) {
      case ALIAS:
        return null == type.toAlias().getType();
      case ENUM:
        return null == type.toEnum().getMembers();
      default:
        type = type.toWrapped().getType();
      }
    }

    // The type has been resolved.
    switch (type.tag()) {
    case VOID:
      // Void always is incomplete.
      return true;
    case ARRAY: {
      // An array is incomplete if (1) it is not variable and has no
      // lenght, (2) it has an incomplete member type, or (3) it has a
      // trailing array.
      ArrayT a = type.toArray();
      return (((! a.isVarLength()) && (! a.hasLength())) ||
              isIncomplete(a.getType()) ||
              hasTrailingArray(a.getType()));
    }
    case STRUCT: {
      // A struct is incomplete if (1) it has no members, (2) any
      // member but the last member is incomplete, (3) the last member
      // is not an array but is incomplete, or (4) the last member is
      // an array with an incomplete element type.
      List<VariableT> members = type.toStruct().getMembers();
      if (null == members) return true;
      for (Iterator<VariableT> iter = members.iterator(); iter.hasNext(); ) {
        VariableT member = iter.next();

        if (iter.hasNext() || (Type.Tag.ARRAY != member.tag())) {
          // In general, members may not be incomplete.  We allow
          // struct members with trailing incomplete arrays because
          // GCC allows them.
          if (isIncomplete(member)) return true;
        } else {
          // The last member is an array.  We allow struct element
          // types with trailing incomplete arrays because GCC allows
          // them.
          ArrayT array = member.resolve().toArray();
          if (isIncomplete(array.getType())) return true;
        }
      }
      return false;
    }
    case UNION: {
      // A union is incomplete if (1) it has no members or (2) any
      // member is incomplete.
      List<VariableT> members = type.toUnion().getMembers();
      if (null == members) return true;
      for (Type t : members) {
        if (isIncomplete(t)) return true;
      }
      return false;
    }
    default:
      return false;
    }
  }

  /**
   * Determine whether the specified type has a trailing array.  This
   * method checks whether the specified type is either a struct type
   * with an incomplete array as its last member or a union containing
   * such a member.
   *
   * @param type The type.
   * @return <code>true</code> if the type has a trailing array.
   */
  public boolean hasTrailingArray(Type type) {
    switch (type.tag()) {
    case STRUCT: {
      // A struct has a trailing array if its last member is a fixed
      // length array without a concrete length.
      List<VariableT> members = type.resolve().toStruct().getMembers();
      if (null != members) {
        int  size = members.size();
        Type last = 0 < size ? members.get(size-1) : null;
        
        if ((null != last) && (Type.Tag.ARRAY == last.tag())) {
          ArrayT a = last.resolve().toArray();
          return (! a.isVarLength()) && (! a.hasLength());
        }
      }
      return false;
    }
    case UNION: {
      // A union has a trailing array if any member has a trailing
      // array.
      List<VariableT> members = type.resolve().toUnion().getMembers();
      if (null != members) {
        for (Type t : members) {
          if (hasTrailingArray(t)) return true;
        }
      }
      return false;
    }
    default:
      return false;
    }
  }

  /**
   * Determine whether the specified type is variably modified.
   * Consistent with C99 6.7.5-3, this method checks whether the type
   * contains a variable length array type.
   *
   * @param type The type.
   * @return <code>true</code> if the type is variably modified.
   */
  public boolean isVariablyModified(Type type) {
    switch (type.tag()) {
    case POINTER:
      return isVariablyModified(type.resolve().toPointer().getType());
    case ARRAY:
      ArrayT a = type.resolve().toArray();
      return a.isVarLength() || isVariablyModified(a.getType());
    default:
      return false;
    }
  }

  /**
   * Determine whether the specified type is qualified as constant.
   * Consistent with C99 6.3.2.1, this method checks whether the type
   * has a {@link Constants#ATT_CONSTANT} attribute and, if it is an
   * array, struct, or union, whether any member or element of the
   * type has that attribute.
   *
   * @param type The type.
   * @return <code>true</code> if the specified type is constant
   *   qualified.
   */
  public boolean isConstant(Type type) {
    if (type.hasAttribute(Constants.ATT_CONSTANT)) return true;

    switch (type.tag()) {
    case ARRAY:
      type = type.resolve().toArray().getType();
      return (null == type) ? false : isConstant(type);
    case STRUCT:
    case UNION:
      for (Type member : type.toTagged().getMembers()) {
        if (isConstant(member)) return true;
      }
      return false;
    default:
      return false;
    }
  }

  /**
   * Determine whether the specified type represents a modifiable
   * lvalue.  Consistent with C99 6.3.2.1, this method checks that the
   * type represents an lvalue, is not incomplete, and is not
   * qualified as constant.  For struct and union types, this method
   * also checks that no member is qualified as constant.
   *
   * @param type The type.
   * @return <code>true</code> if the type represents a modifiable
   *   lvalue.
   */
  public boolean isModifiable(Type type) {
    if (! type.hasShape()) return false;
    if (isIncomplete(type)) return false;
    if (Type.Tag.ARRAY == type.tag()) return false;
    return ! isConstant(type);
  }

  // =========================================================================

  /**
   * Determine whether this type has a constant reference.  This
   * method takes the pointer decay of arrays and functions into
   * account and returns <code>true</code> either if the specified
   * type has a constant reference or if this type is an array or
   * function and has a constant reference.
   *
   * @param type The type.
   * @return <code>true</code> if the type has a constant reference.
   */
  public boolean hasConstRef(Type type) {
    if (type.hasConstant() && type.getConstant().isReference()) return true;

    switch (type.tag()) {
    case ARRAY:
    case FUNCTION:
      return type.hasShape() && type.getShape().isConstant();
    default:
      return false;
    }
  }

  /**
   * Get the specified type's constant reference.
   *
   * @param type The type.
   * @return The constant reference.
   * @throws IllegalArgumentException Signals that the type does not
   *   have a constant reference.
   */
  public Reference getConstRef(Type type) {
    if (type.hasConstant()) {
      Constant constant = type.getConstant();
      if (! constant.isReference()) {
        throw new IllegalArgumentException("Constant not a reference " + type);
      }
      return constant.refValue();
    }

    switch (type.tag()) {
    case ARRAY:
    case FUNCTION:
      if (type.hasShape()) {
        Reference ref = type.getShape();
        if (! ref.isConstant()) {
          throw new IllegalArgumentException("Shaped not constant " + type);
        }
        return ref;
      }
      // Fall through.
    default:
      throw new
        IllegalArgumentException("Type without constant reference " + type);
    }
  }

  // =========================================================================

  /**
   * Determine whether the specified type has any qualifiers.
   *
   * @param type The type.
   * @return <code>true</code> if the type has any qualifiers.
   */
  public boolean hasQualifiers(Type type) {
    return (type.hasAttribute(Constants.ATT_CONSTANT) ||
            type.hasAttribute(Constants.ATT_RESTRICT) ||
            type.hasAttribute(Constants.ATT_VOLATILE));
  }

  /**
   * Determine whether the specified type has at least the qualifiers
   * of the specified template.
   *
   * @param type The type.
   * @param template The template.
   * @return <code>true</code> if the type has at least the template's
   *   qualifiers.
   */
  public boolean hasQualifiers(Type type, Type template) {
    if (template.hasAttribute(Constants.ATT_CONSTANT) &&
        (! type.hasAttribute(Constants.ATT_CONSTANT))) {
      return false;
    }

    if (template.hasAttribute(Constants.ATT_RESTRICT) &&
        (! type.hasAttribute(Constants.ATT_RESTRICT))) {
      return false;
    }

    if (template.hasAttribute(Constants.ATT_VOLATILE) &&
        (! type.hasAttribute(Constants.ATT_VOLATILE))) {
      return false;
    }

    return true;
  }

  /**
   * Determine whether the specified type has the same qualifiers as
   * the specified template.
   *
   * @param type The type.
   * @param template The template.
   * @return <code>true</code> if the type has the same qualifiers
   *   as the template.
   */
  public boolean hasSameQualifiers(Type type, Type template) {
    return ((type.hasAttribute(Constants.ATT_CONSTANT) ==
             template.hasAttribute(Constants.ATT_CONSTANT)) &&
            (type.hasAttribute(Constants.ATT_RESTRICT) ==
             template.hasAttribute(Constants.ATT_RESTRICT)) &&
            (type.hasAttribute(Constants.ATT_VOLATILE) ==
             template.hasAttribute(Constants.ATT_VOLATILE)));
  }

  /**
   * Qualify the specified type with the qualifiers of the specified
   * template.  If the template has any qualifiers, this method first
   * {@link Type#annotate() annotates} the specified type first.
   *
   * @param type The type.
   * @param template The template.
   * @return The qualified type.
   */
  public Type qualify(Type type, Type template) {
    if (! hasQualifiers(template)) return type;

    type = type.annotate();
    if (template.hasAttribute(Constants.ATT_CONSTANT)) {
      type = type.attribute(Constants.ATT_CONSTANT);
    }
    if (template.hasAttribute(Constants.ATT_RESTRICT)) {
      type = type.attribute(Constants.ATT_RESTRICT);
    }
    if (template.hasAttribute(Constants.ATT_VOLATILE)) {
      type = type.attribute(Constants.ATT_VOLATILE);
    }
    return type;
  }

  // =========================================================================

  /**
   * Reattribute the specified type with the specified template's GCC
   * attributes.
   *
   * @param type The type.
   * @param template The template.
   * @return The reattributed type.
   */
  public Type reattribute(Type type, Type template) {
    boolean annotated = false;

    do {
      // If the template has any attributes, check them out.
      if (template.hasAttributes()) {
        for (Attribute att : template.attributes()) {
          if (Constants.NAME_GCC.equals(att.getName()) &&
              (! type.hasAttribute(att))) {
            if (! annotated) {
              type      = type.annotate();
              annotated = true;
            }
            type.addAttribute(att);
          }
        }
      }

      // If the template is a wrapped type, continue with the wrapped type.
      template = template.isWrapped() ? template.toWrapped().getType() : null;
    } while (null != template);

    return type;
  }

  // =========================================================================

  /**
   * Convert the specified type to an rvalue.  If the specified type
   * has a shape, this method returns the resolved type, annotated
   * with any qualifiers and constant value.  Otherwise, it returns
   * the specified type.
   *
   * @param type The type.
   * @return The type as an rvalue.
   */
  public Type toRValue(Type type) {
    if (! type.hasShape()) return type;

    Type result = type.hasEnum() ? type.toEnum() : type.resolve();
    if (hasQualifiers(type) || type.hasConstant()) {
      result = qualify(result.annotate(), type);
      if (type.hasConstant()) {
        result = result.constant(type.getConstant().getValue());
      }
    }
    return result;
  }

  // =========================================================================

  /** The memoized thread-local flag. */
  private Boolean threadlocal = null;

  /**
   * Determine whether the current target supports thread-local
   * storage.
   *
   * @return <code>true</code> if the current target supports
   *   thread-local storage.
   */
  public boolean hasThreadLocals() {
    if (null == threadlocal) {
      threadlocal = Limits.IS_ELF ? Boolean.TRUE : Boolean.FALSE;
    }

    return threadlocal;
  }

  // =========================================================================

  /**
   * Get the specified type's alignment in bytes.
   *
   * @param type The type.
   * @return The type's alignment.
   * @throws IllegalArgumentException Signals that the type does not
   *   have a static alignment.
   */
  public long getAlignment(Type type) {
    return getAlignment(type, true);
  }

  /**
   * Get the specified type's alignment in bytes.  A type's natural
   * alignment is the type's alignment outside arrays, structures, and
   * unions.
   *
   * @param type The type.
   * @param natural The flag for determining the natural alignment.
   * @return The type's alignment.
   * @throws IllegalArgumentException
   *   Signals that the type does not have a static alignment.
   */
  public long getAlignment(Type type, boolean natural) {
    final Type resolved = type.resolve();

    if (resolved.isStruct() || resolved.isUnion()) {
      if (isPacked(type)) {
        // If the structure or union is packed, its alignment is 1.
        return 1;
      } else {
        // Otherwise, the alignment is the maximum of the overall
        // alignment and the members' alignments.
        long alignment = Math.max(1, getAligned(type));
        for (Type t : type.toTagged().getMembers()) {
          // Bit-fields with a zero width do not count.
          if (0 != t.toVariable().getWidth()) {
            alignment = Math.max(alignment, getAlignment(t, false));
          }
        }
        return alignment;
      }
    }

    final long alignment = getAligned(type);
    if (-1 != alignment) return alignment;

    switch (type.tag()) {
    case VOID:
      return Limits.VOID_ALIGN;

    case BOOLEAN:
      return natural ? Limits.BOOL_NAT_ALIGN : Limits.BOOL_ALIGN;

    case INTEGER:
    case FLOAT:
      switch (type.resolve().toNumber().getKind()) {
      case CHAR:
      case S_CHAR:
      case U_CHAR:
        return 1;
      case SHORT:
      case U_SHORT:
        return natural ? Limits.SHORT_NAT_ALIGN : Limits.SHORT_ALIGN;
      case INT:
      case S_INT:
      case U_INT:
        return natural ? Limits.INT_NAT_ALIGN : Limits.INT_ALIGN;
      case LONG:
      case U_LONG:
        return natural ? Limits.LONG_NAT_ALIGN : Limits.LONG_ALIGN;
      case LONG_LONG:
      case U_LONG_LONG:
        return natural ? Limits.LONG_LONG_NAT_ALIGN : Limits.LONG_LONG_ALIGN;
      case FLOAT:
        return natural ? Limits.FLOAT_NAT_ALIGN : Limits.FLOAT_ALIGN;
      case DOUBLE:
        return natural ? Limits.DOUBLE_NAT_ALIGN : Limits.DOUBLE_ALIGN;
      case LONG_DOUBLE:
        return natural ? Limits.LONG_DOUBLE_NAT_ALIGN : Limits.LONG_DOUBLE_ALIGN;
      case FLOAT_COMPLEX:
        return natural ? Limits.FLOAT_NAT_ALIGN : Limits.FLOAT_ALIGN;
      case DOUBLE_COMPLEX:
        return natural ? Limits.DOUBLE_NAT_ALIGN : Limits.DOUBLE_ALIGN;
      case LONG_DOUBLE_COMPLEX:
        return natural ? Limits.LONG_DOUBLE_NAT_ALIGN : Limits.LONG_DOUBLE_ALIGN;
      default:
        throw new
          AssertionError("Invalid number kind " + type.toNumber().getKind());
      }

    case POINTER:
      return natural ? Limits.POINTER_NAT_ALIGN : Limits.POINTER_ALIGN;

    case ARRAY:
      return getAlignment(type.resolve().toArray().getType(), natural);

    case FUNCTION:
      return Limits.FUNCTION_ALIGN;

    default:
      throw new IllegalArgumentException("Type without alignment " + type);
    }
  }

  // =========================================================================

  /**
   * Get the specified type's size in bytes.
   *
   * @param type The type.
   * @return The type's size.
   * @throws IllegalArgumentException Signals that the type does not
   *   have a static size.
   */
  public long getSize(Type type) {
    switch (type.tag()) {
    case VOID:
      return Limits.VOID_SIZE;

    case BOOLEAN:
      return Limits.BOOL_SIZE;

    case INTEGER:
    case FLOAT:
      switch (type.resolve().toNumber().getKind()) {
      case CHAR:
      case S_CHAR:
      case U_CHAR:
        return 1;
      case SHORT:
      case U_SHORT:
        return Limits.SHORT_SIZE;
      case INT:
      case S_INT:
      case U_INT:
        return Limits.INT_SIZE;
      case LONG:
      case U_LONG:
        return Limits.LONG_SIZE;
      case LONG_LONG:
      case U_LONG_LONG:
        return Limits.LONG_LONG_SIZE;
      case FLOAT:
        return Limits.FLOAT_SIZE;
      case DOUBLE:
        return Limits.DOUBLE_SIZE;
      case LONG_DOUBLE:
        return Limits.LONG_DOUBLE_SIZE;
      case FLOAT_COMPLEX:
        return 2 * Limits.FLOAT_SIZE;
      case DOUBLE_COMPLEX:
        return 2 * Limits.DOUBLE_SIZE;
      case LONG_DOUBLE_COMPLEX:
        return 2 * Limits.LONG_DOUBLE_SIZE;
      default:
        throw new
          AssertionError("Invalid number kind " + type.toNumber().getKind());
      }

    case POINTER:
      return Limits.POINTER_SIZE;

    case ARRAY: {
      ArrayT array = type.resolve().toArray();
      if (array.hasLength()) {
        return getSize(array.getType()) * array.getLength();
      } else {
        throw new IllegalArgumentException("Array without size");
      }
    }

    case STRUCT:
      return layout(type.resolve().toStruct(), null);

    case UNION: {
      long size = 0;
      for (Type t : type.toTagged().getMembers()) {
        size = Math.max(size, getSize(t));
      }
      return size;
    }

    case FUNCTION:
      return Limits.FUNCTION_SIZE;

    default:
      throw new IllegalArgumentException("Type without size " + type);
    }
  }

  // =========================================================================

  /**
   * Get the specified member's offset for the specified structure or
   * union.
   *
   * @param type The structure or union.
   * @param name The member's name.
   * @return The offset or -1 if the structure or union does not have
   *   a member with the specified name or the member is a bit-field.
   */
  public long getOffset(StructOrUnionT type, String name) {
    if (type.isStruct()) {
      return layout(type.toStruct(), name);
    } else {
      for (VariableT var : type.getMembers()) {
        if (var.hasName(name)) {
          return 0;
        } else if (! var.hasName() && ! var.hasWidth()) {
          final long offset = getOffset(var.toStructOrUnion(), name);
          if (-1 != offset) return offset;
        }
      }
      return -1;
    }
  }

  // =========================================================================

  /**
   * Lay out the specified structure.
   *
   * @param type The structure.
   * @param name The member name or <code>null</code> for the entire
   *   structure.
   * @return The offset/size or -1 if the structure does not have a
   *   member with the specified name or the member is a bit-field.
   */
  public long layout(StructT type, String name) {
    final List<VariableT> members     = type.getMembers();
    final int             memberCount = members.size();
    final boolean         hasTrailing = hasTrailingArray(type);
    final boolean         isPacked    = isPacked(type);

    long size     = 0;
    long bitCount = 0;
    long bitSize  = 0;
    long bitAlign = 1;
    long maxAlign = Math.max(1, getAligned(type));

    for (int i=0; i<memberCount; i++) {
      // Ignore any trailing array.
      if (hasTrailing && i == memberCount-1) break;

      // Process the member.
      final VariableT var = members.get(i);

      // Determine whether this member is the last one affecting the
      // structure's size.
      final boolean isLastMember =
        i == memberCount-1 || (hasTrailing && i == memberCount-2);

      // Determine the member's alignment.
      final long a = isPacked ? 1 : getAlignment(var, false);
      if (0 != var.getWidth()) maxAlign = Math.max(a, maxAlign);

      if (! var.hasKind(VariableT.Kind.BITFIELD)) {
        // Process the regular member.
        final long mod = size % a;
        if (0 != mod) size += (a - mod);

        if (null != name) {
          if (var.hasName(name)) {
            return size;
          } else if (! var.hasName()) {
            final long offset = getOffset(var.toStructOrUnion(), name);
            if (-1 != offset) return size + offset;
          }
        }

        size += getSize(var);

      } else {
        // Process bit-field.
        final int width = var.getWidth();
        assert -1 < width;

        if (isPacked) {
          bitCount += width;

          if (isLastMember || -1 == members.get(i+1).getWidth()) {
            size += (bitCount / Limits.CHAR_BITS);
            if (0 != bitCount % Limits.CHAR_BITS) size++;

            bitCount = 0;
            bitSize  = 0;
            bitAlign = 1;
          }

        } else if (0 == width) {
          size += (bitCount / Limits.CHAR_BITS);
          if (0 != bitCount % Limits.CHAR_BITS) size++;

          // Use this member's alignment for padding.
          final long mod = size % a;
          if (0 != mod) size += (a - mod);

          bitCount = 0;
          bitSize  = 0;
          bitAlign = 1;

        } else {
          if (0 == bitSize) {
            bitCount = width;
            bitSize  = getSize(var);
            bitAlign = a;

          } else if (bitCount + width <= Limits.toWidth(bitSize)) {
            bitCount += width;

          } else {
            size += bitSize;

            // Use the original member's alignment for padding.
            final long mod = size % bitAlign;
            if (0 != mod) size += (bitAlign - mod);

            bitCount = width;
            bitSize  = getSize(var);
            bitAlign = a;
          }

          if (isLastMember || -1 == members.get(i+1).getWidth()) {
            size += (bitCount / Limits.CHAR_BITS);
            if (0 != bitCount % Limits.CHAR_BITS) size++;

            // Any necessary padding is either emitted when processing
            // the next non-bit-field member or when completing the
            // size calculation through the code below.

            bitCount = 0;
            bitSize  = 0;
            bitAlign = 1;
          }
        }
      }
    }

    if (null != name) return -1;

    // Account for padding to comply with the maximum alignment.
    final long mod = size % maxAlign;
    if (0 != mod) size += (maxAlign - mod);

    return size;
  }

  /** The canonical GCC packed attribute. */
  private static final Attribute PACKED = 
    new Attribute(Constants.NAME_GCC, new Attribute("packed", null));

  /**
   * Determine whether the specified type has a GCC packed attribute.
   *
   * @param type The type.
   * @return <code>true</code> if the type has a packed attribute.
   */
  protected boolean isPacked(Type type) {
    return type.hasAttribute(PACKED);
  }

  /**
   * Get the specified type's alignment.  If the specified type has a
   * GCC aligned attribute, this method returns the corresponding
   * alignment.  Otherwise, it returns -1.
   *
   * @param type The type.
   * @return The alignment or -1 if the type does not have an aligned
   *   attribute.
   */
  protected long getAligned(Type type) {
    long alignment = -1;

    do {
      for (Attribute att : type.attributes()) {
        if (Constants.NAME_GCC.equals(att.getName())) {
          att = (Attribute)att.getValue();
          if ("aligned".equals(att.getName())) {
            if (null == att.getValue()) {
              alignment =
                Math.max(Limits.LONG_LONG_ALIGN, Limits.LONG_DOUBLE_ALIGN);
            } else {
              alignment = ((BigInteger)att.getValue()).longValue();
            }
          }
        }
      }

      if (type.isWrapped()) {
        type = type.toWrapped().getType();
      } else {
        break;
      }
    } while (true);

    return alignment;
  }

  // =========================================================================

  /**
   * Get the specified number's size in bits.
   *
   * @param number The number.
   * @return The size in bits.
   */
  public long getWidth(Type number) {
    switch (number.tag()) {
    case BOOLEAN:
    case INTEGER:
    case FLOAT:
      return getSize(number) * Limits.CHAR_BITS;
    default:
      throw new AssertionError("Not a C number " + number);
    }
  }

  /**
   * Fit the specified number to the closest integer type, starting
   * with <code>int</code>.
   *
   * @param number The number.
   * @return The closest containing integer type or {@link
   *   ErrorT#TYPE} if the number is too large.
   */
  public Type fit(BigInteger number) {
    if (Limits.fitsInt(number)) {
      return NumberT.INT;
    } else if (Limits.fitsUnsignedInt(number)) {
      return NumberT.U_INT;
    } else if (Limits.fitsLong(number)) {
      return NumberT.LONG;
    } else if (Limits.fitsUnsignedLong(number)) {
      return NumberT.U_LONG;
    } else if (Limits.fitsLongLong(number)) {
      return NumberT.LONG_LONG;
    } else if (Limits.fitsUnsignedLongLong(number)) {
      return NumberT.U_LONG_LONG;
    } else {
      return ErrorT.TYPE;
    }
  }

  /**
   * Determine whether the specified number fits the specified integer
   * type.
   *
   * @param number The number.
   * @param type The integer type.
   * @return <code>true</code> if the number fits the type.
   */
  public boolean fits(BigInteger number, Type type) {
    switch (type.tag()) {
    case BOOLEAN:
      return Limits.fitsUnsignedChar(number);
    case INTEGER:
      switch (type.resolve().toInteger().getKind()) {
      case CHAR:
        return Limits.IS_CHAR_SIGNED?
          Limits.fitsChar(number) : Limits.fitsUnsignedChar(number);
      case S_CHAR:
        return Limits.fitsChar(number);
      case U_CHAR:
        return Limits.fitsUnsignedChar(number);
      case SHORT:
        return Limits.fitsShort(number);
      case U_SHORT:
        return Limits.fitsUnsignedShort(number);
      case INT:
      case S_INT:
        return Limits.fitsInt(number);
      case U_INT:
        return Limits.fitsUnsignedInt(number);
      case LONG:
        return Limits.fitsLong(number);
      case U_LONG:
        return Limits.fitsUnsignedLong(number);
      case LONG_LONG:
        return Limits.fitsLongLong(number);
      case U_LONG_LONG:
        return Limits.fitsUnsignedLongLong(number);
      }
    default:
      throw new AssertionError("Not a C integer " + type);
    }
  }

  /**
   * Mask the specified number as a value of this integer type.
   *
   * @param number The number.
   * @param type The type.
   * @return The number masked as a value of this type.
   */
  public BigInteger mask(BigInteger number, Type type) {
    switch (type.tag()) {
    case BOOLEAN:
      return (0 != number.signum()) ? BigInteger.ONE : BigInteger.ZERO;
    case INTEGER:
      switch (type.resolve().toInteger().getKind()) {
      case CHAR:
        return Limits.IS_CHAR_SIGNED ? Limits.maskAsSignedChar(number) :
        Limits.maskAsUnsignedChar(number);
      case S_CHAR:
        return Limits.maskAsSignedChar(number);
      case U_CHAR:
        return Limits.maskAsUnsignedChar(number);
      case SHORT:
        return Limits.maskAsShort(number);
      case U_SHORT:
        return Limits.maskAsUnsignedShort(number);
      case INT:
      case S_INT:
        return Limits.maskAsInt(number);
      case U_INT:
        return Limits.maskAsUnsignedInt(number);
      case LONG:
        return Limits.maskAsLong(number);
      case U_LONG:
        return Limits.maskAsUnsignedLong(number);
      case LONG_LONG:
        return Limits.maskAsLongLong(number);
      case U_LONG_LONG:
        return Limits.maskAsUnsignedLongLong(number);
      }
    default:
      throw new AssertionError("Not a C integer " + type);
    }
  }

  // =========================================================================

  /**
   * Get the specified type's designation.
   *
   * @param type The type.
   * @return The designation.
   */
  public String toDesignation(Type type) {
    switch (type.tag()) {
    case BOOLEAN:
    case INTEGER:
    case FLOAT:
    case POINTER:
      return "scalar";
    case ARRAY:
      return "array";
    case STRUCT:
      return "struct";
    case UNION:
      return "union";
    case FUNCTION:
      return "function";
    case INTERNAL:
      return type.resolve().toInternal().getName();
    default:
      throw new AssertionError("Not a C type " + type);
    }
  }

  // =========================================================================

  /**
   * Integer promote the specified type.  This method resolves the
   * type and, if the type is integral, then performs C's integer
   * promotion (C99 6.3.1.1).  Additionally, it normalizes implicit
   * and signed int types to int types.
   *
   * @param type The type.
   * @return The integer promoted type.
   */
  public Type promote(Type type) {
    // Flag for whether the type represents a bit-field and int is not
    // signed.
    boolean flip = ((! Limits.IS_INT_SIGNED) &&
                    type.hasVariable() &&
                    type.toVariable().hasWidth());

    type = type.resolve();

    switch (type.tag()) {
    case BOOLEAN:
      return NumberT.INT;
    case INTEGER:
      switch (type.toInteger().getKind()) {
      case CHAR:
      case S_CHAR:
      case U_CHAR:
      case SHORT:
        // Shorts and types of lesser rank always fit into an int.
        return NumberT.INT;
      case U_SHORT:      
        if (Limits.SHORT_SIZE < Limits.INT_SIZE) {
          // Unsigned shorts fit into regular ints if their size is
          // smaller.
          return NumberT.INT;
        } else {
          return NumberT.U_INT;
        }
      case INT:
        return flip ? NumberT.U_INT : NumberT.INT;
      case S_INT:
        return NumberT.INT; // Normalize to INT.
      case U_INT:
      case LONG:
      case U_LONG:
      case LONG_LONG:
      case U_LONG_LONG:
        // Nothing to promote.
        return type;
      default:
        throw new AssertionError("Not a C integer " + type);
      }
    default:
      return type;
    }
  }

  /**
   * Argument promote this type.  This method resolves the type and,
   * if the type is an integral type or a float, then performs C's
   * default argument promotions (C99 6.5.2.2).
   *
   * @param type The type.
   * @return The argument promoted type.
   */
  public Type promoteArgument(Type type) {
    Type resolved = type.resolve();

    if (resolved.isFloat()) {
      if (NumberT.Kind.FLOAT == resolved.toFloat().getKind()) {
        return NumberT.DOUBLE;
      } else {
        return resolved;
      }
    } else {
      return promote(type);
    }
  }

  /**
   * Pointerize the specified type.  This method resolves the type
   * and, if the type is an array or function, then performs C's
   * pointer decay (C99 6.3.2.1).
   *
   * @param type The type.
   * @return The pointerized type.
   */
  public Type pointerize(Type type) {
    type = type.resolve();

    switch (type.tag()) {
    case ARRAY:
      return new PointerT(type.toArray().getType());
    case FUNCTION:
      return new PointerT(type);
    default:
      return type;
    }
  }

  // =========================================================================

  /**
   * Perform the usual arithmetic conversions.  Per C99 6.3.1.8, this
   * method performs the usual arithmetic conversions for the
   * specified two types and returns the type of the corresponding
   * result.
   *
   * @param t1 The first type.
   * @param t2 The second type.
   * @return The converted type.
   * @throws IllegalArgumentException Signals that either type is
   *   not arithmetic.
   */
  public Type convert(Type t1, Type t2) {
    // We can't combine non-arithmetic types.
    if ((! isArithmetic(t1)) || t1.hasError()) {
      throw new IllegalArgumentException("Not an arithmetic type " + t1);
    } else if ((! isArithmetic(t2)) || t2.hasError()) {
      throw new IllegalArgumentException("Not an arithmetic type " + t2);
    }

    // Promote the types.  Note that promotion turns enums,
    // enumerators, and bit-fields into integers.
    t1 = promote(t1);
    t2 = promote(t2);

    // Now, we are left with integers and floats.
    NumberT.Kind k1 = ((NumberT)t1).getKind();
    NumberT.Kind k2 = ((NumberT)t2).getKind();

    // First, we convert any complex types.
    if ((! isReal(t1)) || (! isReal(t2))) {
      if ((NumberT.Kind.LONG_DOUBLE_COMPLEX == k1) ||
          (NumberT.Kind.LONG_DOUBLE_COMPLEX == k2)) {
        return NumberT.LONG_DOUBLE_COMPLEX;

      } else if ((NumberT.Kind.DOUBLE_COMPLEX == k1) ||
                 (NumberT.Kind.DOUBLE_COMPLEX == k2)) {
        return NumberT.DOUBLE_COMPLEX;
        
      } if ((NumberT.Kind.FLOAT_COMPLEX == k1) ||
            (NumberT.Kind.FLOAT_COMPLEX == k2)) {
        return NumberT.FLOAT_COMPLEX;
      }
    }

    // Next, we convert any real types.
    if ((NumberT.Kind.LONG_DOUBLE == k1) ||
        (NumberT.Kind.LONG_DOUBLE == k2)) {
      return NumberT.LONG_DOUBLE;

    } else if ((NumberT.Kind.DOUBLE == k1) ||
               (NumberT.Kind.DOUBLE == k2)) {
      return NumberT.DOUBLE;

    } else if ((NumberT.Kind.FLOAT == k1) ||
               (NumberT.Kind.FLOAT == k2)) {
      return NumberT.FLOAT;
    }

    // Otherwise, the integer promotions are performed... Well, they
    // have already been performed above and we are guaranteed to have
    // integers here.
    IntegerT i1 = t1.toInteger();
    IntegerT i2 = t2.toInteger();

    // If both operands have the same type, ...
    if (k1 == k2) return i1;

    // Otherwise,...
    if (i1.isSigned() == i2.isSigned()) {
      return (k1.ordinal() < k2.ordinal()) ? i2 : i1;
    }

    // Otherwise,...
    if (! i1.isSigned()) {
      if (k1.ordinal() > k2.ordinal()) {
        return i1;
      }
    } else {
      if (k2.ordinal() > k1.ordinal()) {
        return i2;
      }
    }

    // Otherwise,...
    if (i1.isSigned()) {
      if (getSize(i1) > getSize(i2)) {
        return i1;
      }
    } else {
      if (getSize(i2) > getSize(i1)) {
        return i2;
      }
    }

    // Otherwise,...
    if (i1.isSigned()) {
      if (NumberT.Kind.INT == k1) {
        return NumberT.U_INT;
      } else if (NumberT.Kind.LONG == k1) {
        return NumberT.U_LONG;
      } else {
        return NumberT.U_LONG_LONG;
      }
    } else {
      if (NumberT.Kind.INT == k2) {
        return NumberT.U_INT;
      } else if (NumberT.Kind.LONG == k2) {
        return NumberT.U_LONG;
      } else {
        return NumberT.U_LONG_LONG;
      }
    }
  }

  // =========================================================================

  /**
   * Compose the specified types.  This method determines whether the
   * two types are compatible while also constructing a composite type
   * as specified in C99 6.2.7.  If the types are compatible, the
   * resulting type is wrapped exactly as the first type.  If the
   * types are not compatible, the resulting type is {@link
   * ErrorT#TYPE}.
   *
   * <p />Note that if both types are derived types, this method
   * ensures that any referenced types have the same qualifiers.
   * However, it does not ensure that the two types have the same
   * qualifiers.  As a result, two types <code>t1</code> and
   * <code>t2</code> are compatible if:
   * <pre>
   * C.hasSameQualfiers(t1, t2) && (! C.compose(t1, t2).isError())
   * </pre>
   *
   * <p />Further note that the composed type does not preserve any
   * annotations or wraped types and thus needs to be annotated with
   * the two type's qualifiers etc.
   *
   * @see #equal(Type,Type)
   *
   * @param t1 The first type.
   * @param t2 The second type.
   * @param pedantic The flag for pedantic composition.
   * @return The composed type.
   */
  public Type compose(Type t1, Type t2, boolean pedantic) {
    return compose(t1, t2, pedantic, true);
  }

  /**
   * Compose the specified types.
   *
   * @param t1 The first type.
   * @param t2 The second type.
   * @param pedantic The flag for pedantic composition.
   * @param recursive The flag for recursive invocations.
   * @return The composed type.
   */
  protected Type compose(Type t1, Type t2, boolean pedantic, boolean recursive) {
    if (recursive) {
      // Preserve any wrapped types.
      if (t1.isEnum()) {
        return t1.equals(t2) ? t1 : ErrorT.TYPE;
        
      } else if (t1.isWrapped()) {
        Type w1 = t1.toWrapped().getType();
        Type c  = compose(w1, t2, pedantic, true);
        
        if (c.isError()) {
          return ErrorT.TYPE;
          
        } else if (w1 == c) {
          return t1;
          
        } else {
          switch (t1.wtag()) {
          case ALIAS:
            return new AliasT(t1, t1.toAlias().getName(), c);
          case ANNOTATED:
            return new AnnotatedT(t1, c);
          case ENUMERATOR: {
            EnumeratorT e = t1.toEnumerator();
            return new EnumeratorT(t1, c, e.getName(), e.getValue());
          }
          case VARIABLE: {
            VariableT v = t1.toVariable();
            return v.hasWidth() ?
              new VariableT(t1, c, v.getName(), v.getWidth())
              : new VariableT(t1, c, v.getKind(), v.getName());
          }
          default:
            throw new AssertionError("Invalid type " + t1);
          }
        }
      }

    } else {
      // Unwrap t1 while still checking enums.
      while (t1.isWrapped()) {
        if (t1.isEnum()) {
          return t1.equals(t2) ? t1 : ErrorT.TYPE;
        } else {
          t1 = t1.toWrapped().getType();
        }
      }
    }

    // t1 has already been resolved; resolve t2 as well.
    t2 = t2.resolve();

    // Make sure both types have the same tag.
    if (t1 == t2) return t1;
    if (t1.tag() != t2.tag()) return ErrorT.TYPE;

    // Now, do the type-specific composition.
    switch (t1.tag()) {
    case ERROR:
      return ErrorT.TYPE;

    case VOID:
    case BOOLEAN:
      return t1;

    case FLOAT:
    case INTEGER:
      return NumberT.equal(t1.toNumber().getKind(), t2.toNumber().getKind()) ?
        t1 : ErrorT.TYPE;

    case INTERNAL:
      return t1.toInternal().getName().equals(t2.toInternal().getName()) ?
        t1 : ErrorT.TYPE;

    case LABEL:
      return t1.toLabel().getName().equals(t2.toLabel().getName()) ?
        t1 : ErrorT.TYPE;

    case STRUCT:
    case UNION:
      return t1 == t2 ? t1 : ErrorT.TYPE;

    case POINTER: {
      // C99 6.7.2, 6.7.5.1
      Type pt1 = t1.toPointer().getType();
      Type pt2 = t2.toPointer().getType();
      if (! hasSameQualifiers(pt1, pt2)) return ErrorT.TYPE;
      Type ptc = compose(pt1, pt2, pedantic, true);
      if (ptc.isError()) return ErrorT.TYPE;
      return pt1 == ptc ? t1 : new PointerT(t1, ptc);
    }

    case ARRAY:
      return composeArrays(t1.toArray(), t2.toArray());

    case FUNCTION:
      return composeFunctions(t1.toFunction(), t2.toFunction(), pedantic);

    default:
      throw new AssertionError("Not a C type " + t1);
    }
  }

  /**
   * Compose the specified array types (C99 6.2.7).
   *
   * @param a1 The first array.
   * @param a2 The second array.
   * @return The composed type.
   */
  protected Type composeArrays(ArrayT a1, ArrayT a2) {
    if (! hasSameQualifiers(a1.getType(), a2.getType())) return ErrorT.TYPE;
    Type el = compose(a1.getType(), a2.getType(), true);
    if (el.isError()) return ErrorT.TYPE;
    
    if (a1.isVarLength()) {
      if (el == a1.getType()) {
        return a1;
      } else {
        return new ArrayT(a1, el, a1.isVarLength(), a1.getLength());
      }
    } else if (a2.isVarLength()) {
      if (el == a2.getType()) {
        return a2;
      } else {
        return new ArrayT(a2, el, a2.isVarLength(), a2.getLength());
      }
    }
    
    if (a1.hasLength() && a2.hasLength()) {
      if (a1.getLength() == a2.getLength()) {
        if (el == a1.getType()) {
          return a1;
        } else {
          return new ArrayT(a1, el, a1.isVarLength(), a1.getLength());
        }
      } else {
        return ErrorT.TYPE;
      }
    }
    
    if (a1.hasLength()) {
      if (el == a1.getType()) {
        return a1;
      } else {
        return new ArrayT(a1, el, a1.isVarLength(), a1.getLength());
      }
    }
    
    if (a2.hasLength()) {
      if (el == a1.getType()) {
        return a2;
      } else {
        return new ArrayT(a2, el, a2.isVarLength(), a2.getLength());
      }
    }
    
    return el == a1.getType() ?
      a1 : new ArrayT(a1, el, a1.isVarLength(), a1.getLength());
  }

  /**
   * Compose the specified function types (C99 6.2.7).  Note that this
   * method ignores any exceptions, which are not part of the C
   * language anyway.
   *
   * @param f1 The first function.
   * @param f2 The second function.
   * @param pedantic The flag for pedantic composition.
   * @return The composed type.
   */
  protected Type composeFunctions(FunctionT f1, FunctionT f2, boolean pedantic) {
    // Compare the names.
    if (null == f1.getName()) {
      if (null != f2.getName()) return ErrorT.TYPE;
    } else {
      if (! f1.getName().equals(f2.getName())) return ErrorT.TYPE;
    }

    // The flag for whether the component types differ from this type.
    boolean differs = false;

    // Compare the results.
    if (! hasSameQualifiers(f1.getResult(), f2.getResult())) return ErrorT.TYPE;
    final Type res = compose(f1.getResult(), f2.getResult(), true);
    if (res.isError()) return ErrorT.TYPE;
    if (f1.getResult() != res) differs = true;

    // Process functions with old-style declarations, since we ignore
    // their parameters.
    if (f1.hasAttribute(Constants.ATT_STYLE_OLD)) {
      if (f2.hasAttribute(Constants.ATT_STYLE_OLD)) {
        // Both types are functions without a parameter type list.
        // However, if type information from the function definition
        // is available, we preserve (but not check) it.
        if (f1.hasAttribute(Constants.ATT_DEFINED) ||
            (! f2.hasAttribute(Constants.ATT_DEFINED))) {
          return differs ?
            new FunctionT(f1, res, f1.getParameters(), f1.isVarArgs()) : f1;
        } else {
          return new FunctionT(f2, res, f2.getParameters(), f2.isVarArgs());
        }

      } else {
        // The other type is a function type with a parameter type
        // list.  It is compatible with this function type as long as
        // it is not variable and this function type is declared.
        if (f2.isVarArgs() && (! f1.hasAttribute(Constants.ATT_DEFINED))) {
          return ErrorT.TYPE;
        } else {
          return new FunctionT(f2, res, f2.getParameters(), f2.isVarArgs());
        }
      }

    } else if (f2.hasAttribute(Constants.ATT_STYLE_OLD)) {
      // This type is a function type with a parameter type list.  It
      // is compatible with the other function type as long as it is
      // not variable and the other function type is declared.
      if (f1.isVarArgs() && (! f2.hasAttribute(Constants.ATT_DEFINED))) {
        return ErrorT.TYPE;
      } else {
        return differs ?
          new FunctionT(f1, res, f1.getParameters(), f1.isVarArgs()) : f1;
      }
    }

    // Neither type is a function type with an old-style declaration.
    // Continue by checking the parameters.
    if (f1.getParameters().size() != f2.getParameters().size()) {
      return ErrorT.TYPE;
    }
    if (f1.isVarArgs() != f2.isVarArgs()) return ErrorT.TYPE;

    final int size = f1.getParameters().size();
    List<Type> par = differs ? new ArrayList<Type>(f1.getParameters()) : null;
    for (int i=0; i<size; i++) {
      final Type p1 = f1.getParameters().get(i);
      final Type p2 = f2.getParameters().get(i);
      if (pedantic && ! hasSameQualifiers(p1, p2)) return ErrorT.TYPE;
      final Type p3 = compose(p1, p2, true);
      if (p3.isError()) return ErrorT.TYPE;

      if (p1 != p3) {
        if (null == par) par = new ArrayList<Type>(f1.getParameters());
        differs = true;
        par.set(i, p3);
      }
    }

    // Ignore the exceptions and we are done.
    if (! differs) return f1;

    if (null == par) par = new ArrayList<Type>(f1.getParameters());

    final FunctionT result = new FunctionT(f1, res, par, f1.isVarArgs());
    return result;
  }

  /**
   * Determine whether the specified types are equal to each other.
   * Calling this method on types <code>t1</code> and <code>t2</code>
   * is equivalent to:
   * <pre>
   * C.hasSameQualifiers(t1, t2) && (! C.compose(t1, t2).isError())
   * </pre>
   *
   * @param t1 The first type.
   * @param t2 The second type.
   * @return <code>true</code> if the types are equal.
   */
  public boolean equal(Type t1, Type t2) {
    return hasSameQualifiers(t1, t2) && (! compose(t1, t2, true).isError());
  }

  // =========================================================================

  /** The factor for chars. */
  protected final BigInteger FACTOR_CHAR =
    BigInteger.valueOf(2).pow(Limits.CHAR_BITS);

  /** The factor for wide chars. */
  protected final BigInteger FACTOR_WIDE =
    BigInteger.valueOf(2).pow(Limits.CHAR_BITS * Limits.WCHAR_SIZE);

  /**
   * Type the specified C character literal.  This method determines
   * the type for the specified character literal, which may be a wide
   * character literal, and returns that type.  The type is annotated
   * with the literal's constant value, even if the value does not fit
   * the type.
   *
   * @param literal The literal.
   * @return The corresponding constant valued type.
   */
  public Type typeCharacter(String literal) {
    // The flag for a wide character literal.
    boolean isWide = false;

    // Strip wide marker and ticks.
    if (literal.startsWith("L")) {
      literal = literal.substring(2, literal.length()-1);
      isWide  = true;
    } else {
      literal = literal.substring(1, literal.length()-1);
    }

    // Unescape.
    literal = Utilities.unescape(literal);

    // Determine the value.
    BigInteger value  = BigInteger.ZERO;
    BigInteger factor = isWide ? FACTOR_WIDE : FACTOR_CHAR;

    final int  length = literal.length();
    for (int i=0; i<length; i++) {
      value = value.multiply(factor).
        add(BigInteger.valueOf(literal.charAt(i)));
    }

    // Determine the type, which according to C99 6.4.4.4 is an int
    // for chars.
    return isWide ? WCHAR.annotate().constant(value) :
      NumberT.CHAR.annotate().constant(value);
  }

  /**
   * Type the specified integer literal.  This method returns the type
   * for the specified C integer literal wrapped in the literal's
   * constant value.  If the specified literal does not fit any type,
   * this method returns the largest appropriate type.
   *
   * @param literal The literal.
   * @return The corresponding type and constant value.
   */
  public Type typeInteger(String literal) {
    // Extract the suffix.
    boolean isUnsigned = false;
    boolean isLong     = false;
    boolean isLongLong = false;
    int     idx        = literal.length();

    for ( ; idx>0; idx--) {
      char c = literal.charAt(idx-1);

      if (('u' == c) || ('U' == c)) {
        isUnsigned = true;

      } else if (('l' == c) || ('L' == c)) {
        if (isLong) {
          isLong     = false;
          isLongLong = true;
        } else {
          isLong     = true;
        }

      } else {
        break;
      }
    }
    literal = literal.substring(0, idx);

    // Extract the radix.
    int radix = 10;

    if (literal.startsWith("0x") || literal.startsWith("0X")) {
      radix   = 16;
      literal = literal.substring(2);

    } else if (literal.startsWith("0")) {
      radix   = 8;
    }

    // Extract the value.
    final BigInteger value = new BigInteger(literal, radix);

    // Find a fitting type (C99 6.4.4.1)
    IntegerT type = null;

    if (isUnsigned) {
      if (isLongLong) {
        if (Limits.fitsUnsignedLongLong(value)) {
          type = NumberT.U_LONG_LONG;
        }

      } else if (isLong) {
        if (Limits.fitsUnsignedLong(value)) {
          type = NumberT.U_LONG;
        } else if (Limits.fitsUnsignedLongLong(value)) {
          type = NumberT.U_LONG_LONG;
        }

      } else {
        if (Limits.fitsUnsignedInt(value)) {
          type = NumberT.U_INT;
        } else if (Limits.fitsUnsignedLong(value)) {
          type = NumberT.U_LONG;
        } else if (Limits.fitsUnsignedLongLong(value)) {
          type = NumberT.U_LONG_LONG;
        }
      }

      // Patch in the biggest type.
      if (null == type) type = NumberT.U_LONG_LONG;

    } else if (10 == radix) {
      if (isLongLong) {
        if (Limits.fitsLongLong(value)) {
          type = NumberT.LONG_LONG;
        }

      } else if (isLong) {
        if (Limits.fitsLong(value)) {
          type = NumberT.LONG;
        } else if (Limits.fitsLongLong(value)) {
          type = NumberT.LONG_LONG;
        }

      } else {
        if (Limits.fitsInt(value)) {
          type = NumberT.INT;
        } else if (Limits.fitsLong(value)) {
          type = NumberT.LONG;
        } else if (Limits.fitsLongLong(value)) {
          type = NumberT.LONG_LONG;
        }
      }

      // Patch in the biggest type.
      if (null == type) type = NumberT.LONG_LONG;

    } else {
      if (isLongLong) {
        if (Limits.fitsLongLong(value)) {
          type = NumberT.LONG_LONG;
        } else if (Limits.fitsUnsignedLongLong(value)) {
          type = NumberT.U_LONG_LONG;
        }

      } else if (isLong) {
        if (Limits.fitsLong(value)) {
          type = NumberT.LONG;
        } else if (Limits.fitsUnsignedLong(value)) {
          type = NumberT.U_LONG;
        } else if (Limits.fitsLongLong(value)) {
          type = NumberT.LONG_LONG;
        } else if (Limits.fitsUnsignedLongLong(value)) {
          type = NumberT.U_LONG_LONG;
        }

      } else {
        if (Limits.fitsInt(value)) {
          type = NumberT.INT;
        } else if (Limits.fitsUnsignedInt(value)) {
          type = NumberT.U_INT;
        } else if (Limits.fitsLong(value)) {
          type = NumberT.LONG;
        } else if (Limits.fitsUnsignedLong(value)) {
          type = NumberT.U_LONG;
        } else if (Limits.fitsLongLong(value)) {
          type = NumberT.LONG_LONG;
        } else if (Limits.fitsUnsignedLongLong(value)) {
          type = NumberT.U_LONG_LONG;
        }
      }

      // Patch in the biggest type.
      if (null == type) type = NumberT.U_LONG_LONG;
    }

    // Done.
    return type.annotate().constant(value);
  }

  /**
   * Type the specified floating point literal.  This method returns
   * the type for the specified C floating point literal wrapped in
   * the literal's constant value.
   *
   * @param literal The literal.
   * @return The corresponding type.
   */
  public Type typeFloat(String literal) {
    char    suffix = literal.charAt(literal.length()-1);
    boolean chop   = false;
    FloatT  type;

    switch (suffix) {
    case 'f':
    case 'F':
      chop = true;
      type = NumberT.FLOAT;
      break;
    case 'l':
    case 'L':
      chop = true;
      type = NumberT.LONG_DOUBLE;
      break;
    case 'd':
    case 'D':
      chop = true;
      // Fall through.
    default:
      type = NumberT.DOUBLE;
    }

    if (chop) literal = literal.substring(0, literal.length()-1);
    return type.annotate().constant(Double.valueOf(literal));
  }

}
