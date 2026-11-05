/*
 *   DENOPTIM
 *   Copyright (C) 2026 Marco Foscato <marco.foscato@uib.no>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package denoptim.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import denoptim.utils.MutationType;

/**
 * Records how a graph or candidate was produced. Only the nearest parents are
 * stored: a crossover keeps its two parents, and a mutation keeps the single
 * candidate it was applied to. Parents of those parents are not copied.
 *
 * @author Marco Foscato
 */
public class Provenance
{

    /**
     * Allowed origins for graphs.
     */
    public enum Source
    {
        CROSSOVER, MUTATION, CONSTRUCTION, MANUAL, INPUT;
    }

//------------------------------------------------------------------------------

    /**
     * One nearest parent, together with the vertices and, for a mutation, the
     * mutation type used to derive the child from that parent.
     */
    public static final class Parent
    {
        private final int generationId;
        private final String candidateId;
        private final int graphId;
        private final List<Long> vertexIds;
        private final Integer vertexPosition;
        private final MutationType mutationType;

        /**
         * @param generationId generation in which the parent was generated.
         * @param candidateId name of the parent candidate. May be {@code null}.
         * @param graphId graph identifier of the parent.
         */
        public Parent(int generationId, String candidateId, int graphId)
        {
            this(generationId, candidateId, graphId, null, null, null);
        }

        /**
         * @param generationId generation in which the parent was generated.
         * @param candidateId name of the parent candidate. May be {@code null}.
         * @param graphId graph identifier of the parent.
         * @param vertexIds vertices of the parent involved in the operation,
         * or {@code null} when none are recorded.
         */
        public Parent(int generationId, String candidateId, int graphId,
                List<Long> vertexIds)
        {
            this(generationId, candidateId, graphId, vertexIds, null, null);
        }

        private Parent(int generationId, String candidateId, int graphId,
                List<Long> vertexIds, Integer vertexPosition,
                MutationType mutationType)
        {
            this.generationId = generationId;
            this.candidateId = candidateId;
            this.graphId = graphId;
            if (vertexIds == null || vertexIds.isEmpty())
            {
                this.vertexIds = Collections.emptyList();
            } else {
                this.vertexIds = Collections.unmodifiableList(
                        new ArrayList<>(vertexIds));
            }
            this.vertexPosition = vertexPosition;
            this.mutationType = mutationType;
        }

        /**
         * @return a copy that records the mutation applied to this parent.
         */
        public Parent withMutation(MutationType mutationType, long vertexId,
                int vertexPosition)
        {
            return new Parent(generationId, candidateId, graphId,
                    List.of(vertexId), vertexPosition, mutationType);
        }

        /**
         * @return generation in which the parent was generated.
         */
        public int getGenerationId()
        {
            return generationId;
        }

        /**
         * @return name of the parent candidate, or {@code null}.
         */
        public String getCandidateId()
        {
            return candidateId;
        }

        /**
         * @return graph identifier of the parent.
         */
        public int getGraphId()
        {
            return graphId;
        }

        /**
         * @return vertices involved in the operation. Never {@code null}.
         */
        public List<Long> getVertexIds()
        {
            return vertexIds;
        }

        /**
         * @return position of the mutated vertex in its graph, or {@code null}
         * when this parent was not mutated.
         */
        public Integer getVertexPosition()
        {
            return vertexPosition;
        }

        /**
         * @return mutation applied to this parent, or {@code null}.
         */
        public MutationType getMutationType()
        {
            return mutationType;
        }

        private Parent normalized()
        {
            return new Parent(generationId, candidateId, graphId, vertexIds,
                    vertexPosition, mutationType);
        }

        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            if (o == null || o.getClass() != getClass())
                return false;
            Parent other = (Parent) o;
            return generationId == other.generationId
                    && graphId == other.graphId
                    && Objects.equals(candidateId, other.candidateId)
                    && vertexIds.equals(other.vertexIds)
                    && Objects.equals(vertexPosition, other.vertexPosition)
                    && mutationType == other.mutationType;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(generationId, candidateId, graphId, vertexIds,
                    vertexPosition, mutationType);
        }

        @Override
        public String toString()
        {
            return JSON.toJson(this);
        }
    }

//------------------------------------------------------------------------------

    /**
     * One parent named by {@link #summary()}. {@code M000111(3)} is candidate
     * {@code M000111} from generation {@code 3}. Enough to find that candidate
     * later, without reading the full provenance from file.
     */
    public static final class ParentLink
    {
        private final String candidateId;
        private final int generationId;

        /**
         * @param candidateId name of the parent candidate.
         * @param generationId generation in which that parent was generated.
         */
        public ParentLink(String candidateId, int generationId)
        {
            if (candidateId == null || candidateId.isBlank())
            {
                throw new IllegalArgumentException(
                        "Parent link candidate id is blank");
            }
            this.candidateId = candidateId;
            this.generationId = generationId;
        }

        /**
         * @return name of the parent candidate.
         */
        public String getCandidateId()
        {
            return candidateId;
        }

        /**
         * @return generation in which the parent was generated.
         */
        public int getGenerationId()
        {
            return generationId;
        }

        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            if (o == null || o.getClass() != getClass())
                return false;
            ParentLink other = (ParentLink) o;
            return generationId == other.generationId
                    && candidateId.equals(other.candidateId);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(candidateId, generationId);
        }

        /**
         * @return {@code candidateId(generationId)}, the token used in
         * {@link Provenance#summary()}.
         */
        @Override
        public String toString()
        {
            return candidateId + "(" + generationId + ")";
        }
    }

//------------------------------------------------------------------------------

    /**
     * Source, mutation types, and parent links read from a {@link #summary()}.
     */
    public static final class Summary
    {
        private final Source source;
        private final List<MutationType> mutationTypes;
        private final List<ParentLink> parents;

        private Summary(Source source, List<MutationType> mutationTypes,
                List<ParentLink> parents)
        {
            this.source = source;
            this.mutationTypes = mutationTypes;
            this.parents = parents;
        }

        /**
         * @return the origin named by the summary.
         */
        public Source getSource()
        {
            return source;
        }

        /**
         * @return mutation types named before the parent list. Never
         * {@code null}. Empty when the summary names none.
         */
        public List<MutationType> getMutationTypes()
        {
            return mutationTypes;
        }

        /**
         * @return parent links, in summary order. Never {@code null}.
         */
        public List<ParentLink> getParents()
        {
            return parents;
        }

        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            if (o == null || o.getClass() != getClass())
                return false;
            Summary other = (Summary) o;
            return source == other.source
                    && mutationTypes.equals(other.mutationTypes)
                    && parents.equals(other.parents);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(source, mutationTypes, parents);
        }
    }

//------------------------------------------------------------------------------

    /**
     * A parent token in {@link #summary()}: candidate id, then generation in
     * parentheses. The id itself contains neither whitespace nor parentheses.
     */
    private static final Pattern PARENT_LINK = Pattern.compile(
            "^([^\\s()]+)\\((\\d+)\\)$");

    /**
     * Compact writer and reader. One JSON object, with no line breaks, so the
     * value fits in a single SDF data line.
     */
    private static final Gson JSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    /**
     * Origin of the graph or candidate.
     */
    private final Source source;

    /**
     * Optional qualifier of {@link #source}. {@code null} when the origin has
     * no qualifier.
     */
    private final String attribute;

    /**
     * Nearest parents only. Empty when this graph was not derived from
     * existing candidates.
     */
    private final List<Parent> parents;

//------------------------------------------------------------------------------

    /**
     * @param source origin of the graph or candidate.
     */
    public Provenance(Source source)
    {
        this(source, null, null);
    }

//------------------------------------------------------------------------------

    /**
     * @param source origin of the graph or candidate.
     * @param attribute qualifier of the origin, or {@code null} when there is
     * none.
     */
    public Provenance(Source source, String attribute)
    {
        this(source, attribute, null);
    }

//------------------------------------------------------------------------------

    /**
     * @param source origin of the graph or candidate.
     * @param parents nearest parents, or {@code null} or an empty list when
     * there are none.
     */
    public Provenance(Source source, List<Parent> parents)
    {
        this(source, null, parents);
    }

//------------------------------------------------------------------------------

    /**
     * @param source origin of the graph or candidate.
     * @param attribute qualifier of the origin, or {@code null} when there is
     * none.
     * @param parents nearest parents, or {@code null} or an empty list when
     * there are none.
     */
    public Provenance(Source source, String attribute, List<Parent> parents)
    {
        if (source == null)
        {
            throw new IllegalArgumentException(
                    "Provenance source must be one of " + Source.class.getName());
        }
        if (attribute != null && attribute.isBlank())
        {
            throw new IllegalArgumentException(
                    "Provenance attribute is blank");
        }
        this.source = source;
        this.attribute = attribute;
        if (parents == null || parents.isEmpty())
        {
            this.parents = Collections.emptyList();
        } else {
            List<Parent> copy = new ArrayList<>();
            for (Parent parent : parents)
            {
                if (parent != null)
                    copy.add(parent.normalized());
            }
            this.parents = Collections.unmodifiableList(copy);
        }
    }

//------------------------------------------------------------------------------

    /**
     * @return construction from fragment space, with no parents.
     */
    public static Provenance construction()
    {
        return new Provenance(Source.CONSTRUCTION);
    }

//------------------------------------------------------------------------------

    /**
     * @param attribute qualifier of the origin, or {@code null} when there is
     * none.
     * @return construction from fragment space, with no parents.
     */
    public static Provenance construction(String attribute)
    {
        return new Provenance(Source.CONSTRUCTION, attribute);
    }
//------------------------------------------------------------------------------

    /**
     * @return a graph introduced manually.
     */
    public static Provenance manual()
    {
        return new Provenance(Source.MANUAL);
    }

//------------------------------------------------------------------------------

    /**
     * @param attribute qualifier of the origin, or {@code null} when there is
     * none.
     * @return a graph introduced manually.
     */
    public static Provenance manual(String attribute)
    {
        return new Provenance(Source.MANUAL, attribute);
    }

//------------------------------------------------------------------------------

    /**
     * @return a graph taken from an input population.
     */
    public static Provenance input()
    {
        return new Provenance(Source.INPUT);
    }

//------------------------------------------------------------------------------

    /**
     * @return a graph taken from an input population.
     */
    public static Provenance input(String attribute)
    {
        return new Provenance(Source.INPUT, attribute);
    }

//------------------------------------------------------------------------------

    /**
     * @param first one nearest parent of the crossover.
     * @param second the other nearest parent of the crossover.
     * @return provenance of a crossover offspring.
     */
    public static Provenance crossover(Parent first, Parent second)
    {
        return new Provenance(Source.CROSSOVER, null, List.of(first, second));
    }

//------------------------------------------------------------------------------

    /**
     * @param parent the candidate that was mutated.
     * @return provenance of a mutant. The mutation site can be added later
     * with {@link #withMutation(MutationType, long, int)}.
     */
    public static Provenance mutation(Parent parent)
    {
        return new Provenance(Source.MUTATION, null, List.of(parent));
    }

//------------------------------------------------------------------------------

    /**
     * Records the mutation site on the nearest parent. Provenance that is not
     * a {@link Source#MUTATION}, or that has no parent, is returned unchanged
     * so construction and crossover do not accumulate parent-of-parent detail.
     * @param mutationType mutation that was performed.
     * @param vertexId vertex that was mutated, in the numbering stored before
     * the graph was renumbered.
     * @param vertexPosition position of that vertex in the graph.
     * @return a provenance with the mutation site on the first parent, or
     * {@code this} when there is no mutation parent to update.
     */
    public Provenance withMutation(MutationType mutationType, long vertexId,
            int vertexPosition)
    {
        if (source != Source.MUTATION || parents.isEmpty())
            return this;
        List<Parent> updated = new ArrayList<>(parents);
        updated.set(0, parents.get(0).withMutation(mutationType, vertexId,
                vertexPosition));
        return new Provenance(source, attribute, updated);
    }

//------------------------------------------------------------------------------

    /**
     * Reads the single-line JSON produced by {@link #toString()}.
     * @param text serialized provenance.
     * @return the parsed provenance, or {@code null} when {@code text} is
     * empty or not a provenance JSON object.
     */
    public static Provenance parse(String text)
    {
        if (text == null || text.isBlank())
            return null;
        try
        {
            Provenance raw = JSON.fromJson(text.trim(), Provenance.class);
            if (raw == null || raw.source == null)
                return null;
            return new Provenance(raw.source, raw.attribute, raw.parents);
        } catch (RuntimeException e) {
            return null;
        }
    }

//------------------------------------------------------------------------------

    /**
     * @return the origin of the graph or candidate.
     */
    public Source getSource()
    {
        return source;
    }

//------------------------------------------------------------------------------

    /**
     * @return the qualifier of the origin, for example {@code Fragment space}, or
     * {@code null} when the origin has none.
     */
    public String getAttribute()
    {
        return attribute;
    }

//------------------------------------------------------------------------------

    /**
     * @return the nearest parents. Never {@code null}. Empty when this graph
     * was not derived from existing candidates.
     */
    public List<Parent> getParents()
    {
        return parents;
    }

//------------------------------------------------------------------------------

    /**
     * Short label for a lightweight candidate. The source comes first, then
     * each mutation type, then each parent as {@code candidateId(generation)}.
     * Parents come last so {@link #parseSummary(String)} can read them off the
     * end. An attribute, graph id, or vertex id is left out; the SDF file
     * still holds the full provenance.
     * @return for example {@code MUTATION CHANGEBRANCH M00000001(3)}.
     */
    public String summary()
    {
        StringBuilder text = new StringBuilder(source.name());
        for (Parent parent : parents)
        {
            if (parent.mutationType != null)
                text.append(' ').append(parent.mutationType.name());
        }
        for (Parent parent : parents)
        {
            String link = parentLinkToken(parent);
            if (link != null)
                text.append(' ').append(link);
        }
        return text.toString();
    }

//------------------------------------------------------------------------------

    /**
     * Reads a {@link #summary()} string.
     * @param text summary produced by {@link #summary()}.
     * @return the source, mutation types, and parent links, or {@code null}
     * when {@code text} is empty or not a summary. The parent links are not
     * resolved to files.
     */
    public static Summary parseSummary(String text)
    {
        if (text == null || text.isBlank())
            return null;
        String[] tokens = text.trim().split("\\s+");
        Source parsedSource;
        try
        {
            parsedSource = Source.valueOf(tokens[0]);
        } catch (IllegalArgumentException e) {
            return null;
        }
        List<MutationType> mutationTypes = new ArrayList<>();
        List<ParentLink> links = new ArrayList<>();
        boolean parentsStarted = false;
        for (int i = 1; i < tokens.length; i++)
        {
            ParentLink link = parentLink(tokens[i]);
            if (link != null)
            {
                parentsStarted = true;
                links.add(link);
                continue;
            }
            if (parentsStarted)
                return null;
            MutationType mutationType = mutationType(tokens[i]);
            if (mutationType == null)
                return null;
            mutationTypes.add(mutationType);
        }
        return new Summary(parsedSource,
                Collections.unmodifiableList(mutationTypes),
                Collections.unmodifiableList(links));
    }

//------------------------------------------------------------------------------

    /**
     * @return {@code candidateId(generation)}, or {@code null} when this parent
     * has no candidate id that can stand as one summary token.
     */
    private static String parentLinkToken(Parent parent)
    {
        if (parent.candidateId == null)
            return null;
        ParentLink link = parentLink(parent.candidateId + "("
                + parent.generationId + ")");
        if (link == null)
            return null;
        return link.toString();
    }

//------------------------------------------------------------------------------

    private static ParentLink parentLink(String token)
    {
        Matcher matcher = PARENT_LINK.matcher(token);
        if (!matcher.matches())
            return null;
        return new ParentLink(matcher.group(1),
                Integer.parseInt(matcher.group(2)));
    }

//------------------------------------------------------------------------------

    private static MutationType mutationType(String token)
    {
        for (MutationType type : MutationType.values())
        {
            if (type.name().equals(token))
                return type;
        }
        return null;
    }

//------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (o == null || o.getClass() != getClass())
            return false;
        Provenance other = (Provenance) o;
        return source == other.source
                && Objects.equals(attribute, other.attribute)
                && parents.equals(other.parents);
    }

//------------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        return Objects.hash(source, attribute, parents);
    }

//------------------------------------------------------------------------------

    /**
     * @return the single-line JSON form of this provenance.
     * {@link #parse(String)} reads it back.
     */
    @Override
    public String toString()
    {
        return JSON.toJson(this);
    }

//------------------------------------------------------------------------------
}
