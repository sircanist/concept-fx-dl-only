package conexp.fx.core.dl;

/*
 * #%L
 * Concept Explorer FX
 * %%
 * Copyright (C) 2010 - 2022 Francesco Kriegel
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class ELReasoner {

  public static final boolean subsumes(final OWLClassExpression concept1, final OWLClassExpression concept2) {
    return isSubsumedBy(concept2, concept1);
  }

  public static final boolean subsumes(final ELConceptDescription concept1, final ELConceptDescription concept2) {
    return isSubsumedBy(concept2, concept1);
  }

  public static final boolean isSubsumedBy(final OWLClassExpression concept1, final OWLClassExpression concept2) {
    return isSubsumedBy(ELConceptDescription.of(concept1), ELConceptDescription.of(concept2));
  }

  /**
   * @param concept1
   * @param concept2
   * @return true, iff concept1 is subsumed by concept2 (w.r.t. empty TBox)
   */
  public static final boolean isSubsumedBy(final ELConceptDescription concept1, final ELConceptDescription concept2) {
    final ELConceptDescription _concept1 = concept1.clone().reduce();
    final ELConceptDescription _concept2 = concept2.clone().reduce();
    if (_concept1.isBot())
      return true;
    if (_concept2.isTop())
      return true;
    if (!_concept1.getConceptNames().containsAll(_concept2.getConceptNames()))
      return false;
    if (!_concept2.getDataValues()
            .entries()
            .parallelStream()
            .allMatch(
                 dataValue2 ->  _concept1.getDataValues()
                    .entries()
                    .parallelStream()
                    .anyMatch(
                            dataValue1 -> dataValue2.getKey().equals(dataValue1.getKey())
                            && dataValue1.getValue().equals(dataValue2.getValue()))))
      return false;
    return _concept2
        .getExistentialRestrictions()
        .entries()
        .parallelStream()
        .allMatch(
            existentialRestriction2 -> _concept1
                .getExistentialRestrictions()
                .entries()
                .parallelStream()
                .anyMatch(
                    existentialRestriction1 -> existentialRestriction2.getKey().equals(existentialRestriction1.getKey())
                        && isSubsumedBy(existentialRestriction1.getValue(), existentialRestriction2.getValue())));
  }

  public static final boolean
      isSubsumedBy(final ELConceptDescription concept1, final ELConceptDescription concept2, final ELTBox tBox) {
    return isSubsumedBy(concept1.toOWLClassExpression(), concept2.toOWLClassExpression(), tBox.toOWLOntology());
  }

  private static int dummy = 1618;

  private static final int nextDummy() {
    return dummy++;
  }

  public static final boolean
      isSubsumedBy(final OWLClassExpression concept1, final OWLClassExpression concept2, final OWLOntology ontology) {
    final OWLOntologyManager om = OWLManager.createOWLOntologyManager();
    final OWLDataFactory df = OWLManager.getOWLDataFactory();

    final OWLClass c1 = df.getOWLClass(IRI.create("dummy" + nextDummy()));
    final OWLClass c2 = df.getOWLClass(IRI.create("dummy" + nextDummy()));
    final OWLSubClassOfAxiom ax1 = df.getOWLSubClassOfAxiom(concept1, c1);
    final OWLSubClassOfAxiom _ax1 = df.getOWLSubClassOfAxiom(c1, concept1);
    final OWLSubClassOfAxiom ax2 = df.getOWLSubClassOfAxiom(c2, concept2);
    final OWLSubClassOfAxiom _ax2 = df.getOWLSubClassOfAxiom(concept2, c2);
    om.applyChange(new AddAxiom(ontology, ax1));
    om.applyChange(new AddAxiom(ontology, ax2));
    om.applyChange(new AddAxiom(ontology, _ax1));
    om.applyChange(new AddAxiom(ontology, _ax2));

    final OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(ontology);
//    final OWLReasoner reasoner = new JcelReasonerFactory().createReasoner(ontology);
    reasoner.flush();
    reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

    final boolean result =
        reasoner.getEquivalentClasses(c2).contains(c1) || reasoner.getSubClasses(c2, false).getFlattened().contains(c1);
    reasoner.dispose();
    om.applyChange(new RemoveAxiom(ontology, ax1));
    om.applyChange(new RemoveAxiom(ontology, ax2));
    om.applyChange(new RemoveAxiom(ontology, _ax1));
    om.applyChange(new RemoveAxiom(ontology, _ax2));
    return result;
//    return elk.isEntailed(df.getOWLSubClassOfAxiom(concept1, concept2));
  }

  public static final boolean isSubsumedBy(
      final ELConceptDescription concept1,
      final ELConceptDescription concept2,
      final OWLOntology ontology) {
    return isSubsumedBy(concept1.toOWLClassExpression(), concept2.toOWLClassExpression(), ontology);
  }

}
