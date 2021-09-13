package game.rules.play.moves;

import java.util.Iterator;
import java.util.function.BiPredicate;

import annotations.Opt;
import game.Game;
import game.rules.meta.NoRepeat;
import game.rules.play.moves.nonDecision.effect.Then;
import game.types.state.GameType;
import main.collections.FastArrayList;
import other.BaseLudeme;
import other.context.Context;
import other.move.Move;
import other.move.MovesIterator;

/**
 * Returns a moves collection.
 * 
 * @author cambolbro and Eric.Piette
 */
public abstract class Moves extends BaseLudeme implements GameType
{
	private static final long serialVersionUID = 1L;

	//-------------------------------------------------------------------------

	/** List of Moves. */
	private final FastArrayList<Move> moves = new FastArrayList<Move>(10);

	/** Consequent actions of moves. */
	private Then then;

	/** If the move is a decision. */
	private boolean isDecision = false;

	/**
	 * Use for the simultaneous games, to apply the consequence when all the moves
	 * of all the players are applied.
	 */
	private boolean applyAfterAllMoves = false;

	//-------------------------------------------------------------------------

	/**
	 * @param then The subsequents of the moves.
	 */
	public Moves
	(
		@Opt final Then then
	)
	{
		this.then = then;
	}

	//-------------------------------------------------------------------------

	/**
	 * @return The moves generated by moves.
	 */
	public FastArrayList<Move> moves()
	{
		return moves;
	}

	/**
	 * @return The subsequents of the moves.
	 */
	public Then then()
	{
		return then;
	}

	/**
	 * To set the then moves of the moves.
	 * 
	 * @param then
	 */
	public void setThen(final Then then)
	{
		this.then = then;
	}

	/**
	 * @return The number of moves.
	 */
	public int count()
	{
		return moves.size();
	}
	
	/**
	 * @param n
	 * @return To get the move n in the list of moves.
	 */
	public Move get(final int n)
	{
		return moves.get(n);
	}

	//-------------------------------------------------------------------------

	/**
	 * @param context
	 * @return The result of applying this function in this context.
	 */
	public abstract Moves eval(final Context context);

	//-------------------------------------------------------------------------
	
	/**
	 * @param context
	 * @param target
	 * @return True if this generator can generate moves in the given context
	 * that would move towards "target"
	 */
	public boolean canMoveTo(final Context context, final int target)
	{
		final Iterator<Move> it = movesIterator(context);
		
		while (it.hasNext())
		{
			if (it.next().toNonDecision() == target)
				return true;
		}
		
		return false;
	}
	
	//-------------------------------------------------------------------------
	
	@Override
	public void preprocess(final Game game)
	{
		if (then != null)
			then.moves().preprocess(game);
	}
	
	@Override
	public long gameFlags(final Game game)
	{
		long result = 0L;
		if (then != null)
			result |= then.moves().gameFlags(game);
		return result;
	}

	@Override
	public boolean isStatic()
	{
		if (then != null)
			return then.moves().isStatic();

		return false;		// TODO this should return true?
	}
	
	//-------------------------------------------------------------------------

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();

		for (final Move m : moves)
		{
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(m.toString());
		}

		if (then != null) 
		{
			sb.append(then);
		}
		
		return sb.toString();
	}
	
	//-------------------------------------------------------------------------

	// WARNING: the below hashCode() and equals() implementations are WRONG!
	// All other Moves ludemes do not provide correct (or any) implementations!
	
//	@Override
//	public int hashCode()
//	{
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + ((consequents == null) ? 0 : consequents.hashCode());
//		result = prime * result + ((moves == null) ? 0 : moves.hashCode());
//		return result;
//	}
//
//	@Override
//	public boolean equals(final Object obj)
//	{
//		if (this == obj)
//			return true;
//		
//		if (obj == null)
//			return false;
//
//		if (getClass() != obj.getClass())
//			return false;
//		
//		final Moves other = (Moves) obj;
//		if (consequents == null)
//		{
//			if (other.consequents != null)
//				return false;
//		}
//		else if (!consequents.equals(other.consequents))
//		{
//			return false;
//		}
//		
//		if (moves == null)
//		{
//			if (other.moves != null)
//				return false;
//		}
//		else if (!moves.equals(other.moves))
//		{
//			return false;
//		}
//		
//		return true;
//	}

	//-------------------------------------------------------------------------

	/**
	 * @return True if the moves is a constraint move.
	 */
	public boolean isConstraintsMoves()
	{
		return false;
	}
	
	/**
	 * @return True if the moves returned have to be a decision moves.
	 */
	public boolean isDecision()
	{
		return isDecision;
	}

	/**
	 * Set the moves to be a decision.
	 */
	public void setDecision()
	{
		isDecision = true;
	}

	/**
	 * @return The applyAfterAllMoves.
	 */
	public boolean applyAfterAllMoves()
	{
		return applyAfterAllMoves;
	}

	/**
	 * Set the flag applyAfterAllMoves.
	 * 
	 * @param value The new value.
	 */
	public void setApplyAfterAllMoves(final boolean value)
	{
		applyAfterAllMoves = value;
	}

	//-------------------------------------------------------------------------
	
	/**
	 * @param context
	 * @return Iterator for iterating through moves in given context.
	 * 	Base class implementation just computes full list of legal moves,
	 * 	and allows iterating through it afterwards.
	 */
	public MovesIterator movesIterator(final Context context)
	{
		final FastArrayList<Move> generatedMoves = eval(context).moves();
		return new MovesIterator(){
			
			private int cursor = 0;

			@Override
			public boolean hasNext()
			{
				return cursor < generatedMoves.size();
			}

			@Override
			public Move next()
			{
				return generatedMoves.get(cursor++);
			}

			@Override
			public boolean canMoveConditionally(final BiPredicate<Context, Move> predicate)
			{
				for (final Move m : generatedMoves)
					if (predicate.test(context, m))
						return true;
				
				return false;
			}
			
		};
	}
	
	/**
	 * @param context
	 * @return True if there is at least one legal move in given context
	 */
	public boolean canMove(final Context context)
	{
		return movesIterator(context).canMoveConditionally(
				(final Context c, final Move m) -> { return NoRepeat.apply(c, m); });
	}
}
