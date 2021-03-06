/*
 * Copyright (c) 2012. John May
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 */

package uk.ac.ebi.centres.graph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import uk.ac.ebi.centres.ConnectionProvider;
import uk.ac.ebi.centres.DescriptorManager;
import uk.ac.ebi.centres.Digraph;
import uk.ac.ebi.centres.Ligand;
import uk.ac.ebi.centres.MutableDescriptor;
import uk.ac.ebi.centres.exception.WarpCoreEjection;
import uk.ac.ebi.centres.ligand.NonterminalLigand;
import uk.ac.ebi.centres.ligand.TerminalLigand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * A digraph with a single immutable root.
 *
 * @author John May
 */
public abstract class AbstractDigraph<A> implements Digraph<A>,
                                                    ConnectionProvider<A> {

    private Ligand<A> root;
    private ArcMap                     arcs      = new ArcMap(); // Could set expected size
    private ListMultimap<A, Ligand<A>> ligandMap = ArrayListMultimap.create();
    private DescriptorManager<A> manager;


    public AbstractDigraph(Ligand<A> root) {
        this(root, new DefaultDescriptorManager<A>());
    }


    public AbstractDigraph(Ligand<A> root, DescriptorManager<A> manager) {
        if (root == null)
            throw new IllegalArgumentException("Root cannot be null!");
        this.root = root;
        this.manager = manager;
    }


    @Override
    public Ligand<A> getRoot() {
        return root;
    }


    @Override
    public List<Ligand<A>> getProximal() {
        return root.getLigands();
    }


    @Override
    public List<Ligand<A>> getLigands(A atom) {
        return ligandMap.get(atom);
    }


    /**
     * @inheritDoc
     */
    @Override
    public void reroot(Ligand<A> ligand) {

//        System.out.println("tails: " + arcs.tails);
//        System.out.println("heads: " + arcs.heads);

        root = ligand;
        ligand.reset();

        Queue<Arc<A>> queue = new LinkedList<Arc<A>>();

        // get parent arcs
        Arc<A> arc = arcs.getForHead(ligand);
        while (arc != null) {
            arcs.remove(arc);
            Arc<A> next = arcs.getForHead(arc.getTail());
            arc.transpose();
            queue.add(arc);
            arc = next;
        }

        for (Arc<A> transposedArc : queue) {
            arcs.add(transposedArc);
        }

        ligand.setParent(ligand.getAtom());

    }


    /**
     * @inheritDoc
     */
    @Override
    public void build() {

        if (root == null)
            throw new IllegalArgumentException("Attempting build without a root");

        Queue<Ligand<A>> queue = new LinkedList<Ligand<A>>();

        queue.addAll(root.getLigands());

        while (!queue.isEmpty()) {
            queue.addAll(queue.poll().getLigands());
        }

    }


    @Override
    public List<Arc<A>> getArcs(Ligand<A> ligand) {
        return arcs.getForTail(ligand);
    }


    @Override
    public Arc<A> getParentArc(Ligand<A> ligand) {
        return arcs.getForHead(ligand);
    }


    @Override
    public List<Ligand<A>> getLigands(Ligand<A> ligand) {

        List<Ligand<A>> ligands = arcs.getHeads(ligand);

        // lots of ligands being created
        if(ligandMap.size() > 10000)
            throw new WarpCoreEjection();

        // ligands already determined
        if (!ligands.isEmpty())
            return ligands;


        // ligands have not be built
        for (A atom : getConnected(ligand.getAtom())) {

            if (ligand.isParent(atom))
                continue;

            MutableDescriptor descriptor = manager.getDescriptor(atom);

            // create the new ligand - terminal ligands are created in cases of cyclic molecules
            Ligand<A> neighbour = ligand.isVisited(atom)
                                  ? new TerminalLigand<A>(this, descriptor, ligand.getVisited(), atom, ligand.getAtom(), ligand.getDistanceFromRoot() + 1)
                                  : new NonterminalLigand<A>(this, descriptor, ligand.getVisited(), atom, ligand.getAtom(), ligand.getDistanceFromRoot() + 1);
            arcs.add(newArc(ligand, neighbour));
            ligandMap.put(atom, neighbour);

            ligands.add(neighbour);

            int order = getOrder(ligand.getAtom(), atom);

            // create ghost ligands (opened up from double bonds)
            if (order > 1) {

                // create required number of ghost ligands
                for (int i = 1; i < order; i++) {
                    Ligand<A> ghost = new TerminalLigand<A>(this, descriptor, ligand.getVisited(), atom, ligand.getAtom(), ligand.getDistanceFromRoot() + 1);
                    arcs.add(newArc(ligand, ghost));
                    ligandMap.put(atom, ghost);
                    ligands.add(ghost);
                }

                // preload the neighbour and add the call back ghost...
                // bit confusing but this turns -c1-c2=c3-o into:
                //          c2
                //         /
                // -c1-c2-c3-o
                //     \
                //      c3
                // when we're at c2 we preload c3 with the oxygen and then add the ghost c2
                getLigands(neighbour);
                Ligand<A> ghost = new TerminalLigand<A>(this, descriptor, ligand.getVisited(), ligand.getAtom(), atom, ligand.getDistanceFromRoot() + 1);
                arcs.add(newArc(neighbour, ghost));
                ligandMap.put(ligand.getAtom(), ghost);

            }

        }

        return ligands;

    }


    public abstract Collection<A> getConnected(A atom);

    public abstract int getOrder(A first, A second);

    public abstract int getDepth(A first, A second);


    private Arc<A> newArc(Ligand<A> tail, Ligand<A> head) {
        return new Arc<A>(tail, head,
                          manager.getDescriptor(tail.getAtom(), head.getAtom()),
                          getDepth(tail.getAtom(), head.getAtom()));
    }


    /**
     * Manages maps of ligands and thier arcs
     */
    class ArcMap {

        private final ListMultimap<Ligand<A>, Arc<A>> tails = ArrayListMultimap.create();
        private final Map<Ligand<A>, Arc<A>>          heads = new HashMap<Ligand<A>, Arc<A>>();


        public void remove(Arc<A> arc) {
            //System.out.println("\tremoving " + arc.getTail() + ": " + arc + " and " + arc.getHead() + ": " + arc);
            tails.remove(arc.getTail(), arc);
            heads.remove(arc.getHead());
        }


        public void add(Arc<A> arc) {
            tails.put(arc.getTail(), arc);
            if (heads.containsKey(arc.getHead()))
                System.err.println("Key clash!");
            heads.put(arc.getHead(), arc);
        }


        public Arc<A> getForHead(Ligand<A> head) {
            return heads.get(head);
        }


        public List<Arc<A>> getForTail(Ligand<A> tail) {
            return tails.get(tail);
        }


        public List<Ligand<A>> getHeads(Ligand<A> tail) {

            // this okay for now but should create a custom list that proxyies calls
            // to the arc list
            List<Arc<A>> arcs = tails.get(tail);
            List<Ligand<A>> ligands = new ArrayList<Ligand<A>>(arcs.size());
            for (Arc<A> arc : arcs) {
                ligands.add(arc.getHead());
            }
            return ligands;

        }


        public Ligand<A> getTail(Ligand<A> head) {
            Arc<A> arc = getForHead(head);
            if (arc == null)
                throw new NoSuchElementException("No tail for provided head");
            return arc.getTail();
        }


    }


    @Override
    public void dispose() {
        ligandMap.clear();
        arcs.tails.clear();
        arcs.heads.clear();
        root = null;
        arcs = null;
        ligandMap = null;
        manager = null;

    }
}
