package edu.umd.cs.psl.model.atom;

import java.util.HashSet;
import java.util.Set;

import edu.umd.cs.psl.database.Database;
import edu.umd.cs.psl.database.DatabaseQuery;
import edu.umd.cs.psl.database.ResultList;
import edu.umd.cs.psl.model.argument.GroundTerm;
import edu.umd.cs.psl.model.argument.Variable;
import edu.umd.cs.psl.model.formula.Formula;
import edu.umd.cs.psl.model.predicate.Predicate;
import edu.umd.cs.psl.model.predicate.StandardPredicate;

/**
 * Implements the {@link AtomManager} with a twist: this atom manager will only return
 * {@link RandomVariableAtom RandomVariableAtoms} that were persisted in the database 
 * at instantiation.
 * <p>
 * All other atoms are returned normally.
 * @author Eric Norris <enorris@cs.umd.edu>
 *
 */
public class PersistedAtomManager implements AtomManager {

	/**
	 * This AtomManager's connection to a database.
	 */
	private final Database db;
	
	/**
	 * The set of all persisted RandomVariableAtoms at the time of this AtomManager's
	 * instantiation.
	 */
	private final Set<RandomVariableAtom> persistedCache;
	
	/**
	 * Returns a PersistedAtomManager with a built-in set of all the database's
	 * persisted RandomVariableAtoms.
	 * @param db	the Database to query for all getAtom() calls.
	 */
	public PersistedAtomManager(Database db) {
		this.db = db;
		this.persistedCache = new HashSet<RandomVariableAtom>();
		
		buildPersistedAtomCache();
	}
	
	private void buildPersistedAtomCache() {
		//TODO Isn't it possible to query for a predicate that is not registered with this database?
		// Iterate through all of the registered predicates in this database
		for (StandardPredicate predicate : db.getDataStore().getRegisteredPredicates()) {
			// Ignore any closed predicates, they will not return RandomVariableAtoms
			if (db.isClosed(predicate))
				continue;
			
			// Construct the query for this predicate
			Variable vars[] = new Variable[predicate.getArity()];
			for (int i = 0; i < vars.length; i++)
				vars[i] = new Variable(String.valueOf(i));
			Formula queryFormula = new QueryAtom(predicate, vars);
			
			// Execute the query and interpret the results
			ResultList list = db.executeQuery(new DatabaseQuery(queryFormula));
			for (int i = 0; i < list.size(); i ++) {
				// Query the database for this specific atom
				GroundAtom atom = db.getAtom(predicate, list.get(i));
				
				// If this is a RandomVariableAtom, store it in our cache
				if (atom instanceof RandomVariableAtom)
					persistedCache.add((RandomVariableAtom)atom);
			}
		}
	}
	
	@Override
	public GroundAtom getAtom(Predicate p, GroundTerm... arguments) {
		GroundAtom atom = db.getAtom(p, arguments);
		if (atom instanceof RandomVariableAtom) {
			// Check if this is in our persisted atom cache
			if (persistedCache.contains(atom))
				return atom;
			else
				throw new IllegalArgumentException("Can only call getAtom() on persisted atoms using a PersistedAtomManager.");
		} else
			return atom;
	}

	@Override
	public Database getDatabase() {
		return db;
	}

}
