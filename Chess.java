import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.concurrent.*;
public class Chess
{
	private final static String dashes="\n  -----------------  \n", lower="\n   a b c d e f g h   "+dashes, upper="   A B C D E F G H   \n";
	private final static int[][] changes={{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}}, knights={{-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}};
	private char[][] board;
	private boolean white;
	private String castle, enPassant, whiteKing, blackKing;
	private static boolean engineWhite;
	private static HashMap<Character, Integer> values=new HashMap<>()
	{{
		put(' ', 0);
		put('r', -5);
		put('n', -3);
		put('b', -3);
		put('q', -9);
		put('k', -300);
		put('p', -1);
		put('R', 5);
		put('N', 3);
		put('B', 3);
		put('Q', 9);
		put('K', 300);
		put('P', 1);
	}};
	private HashMap<String, Integer> positions;
	private int total;
	private String[][] killerMoves;
	private ArrayList<String> movesPlayed, fixedMoves;
	private static ConcurrentHashMap<String, Integer> checkedBoards;
	private String stringify(int r, int c)
	{
		return ""+(char)('a'+c)+(8-r);
	}
	public Chess(Chess c)
	{
		board=new char[8][8];
		for(int i=0; i<8; i++)
			for(int j=0; j<8; j++)
				board[i][j]=c.board[i][j];
		white=c.white;
		castle=c.castle;
		enPassant=c.enPassant;
		whiteKing=c.whiteKing;
		blackKing=c.blackKing;
		total=c.total;
		killerMoves=new String[c.killerMoves.length][2];
	}
	public Chess(String fen, int depth)
	{
		String[] f=fen.split(" ");
		String[] l=Pattern.compile("\\d").matcher(f[0]).replaceAll(mr->" ".repeat(Integer.parseInt(mr.group()))).split("/");
		board=new char[8][8];
		for(int i=0; i<8; i++)
		{
			int j=l[i].indexOf("K");
			if(j>=0)
				whiteKing=stringify(i, j);
			j=l[i].indexOf("k");
			if(j>=0)
				blackKing=stringify(i, j);
			board[i]=l[i].toCharArray();
		}
		white=f[1].equals("w");
		castle=f[2];
		enPassant=f[3];
		positions=new HashMap<>();
		total=0;
		for(int i=0; i<8; i++)
			for(int j=0; j<8; j++)
				total+=values.get(board[i][j]);
		killerMoves=new String[depth][2];
		movesPlayed=new ArrayList<>();
		fixedMoves=new ArrayList<>();
		engineWhite=true;
	}
	public Chess(int depth)
	{
		this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -", depth);
	}
	private String boardString()
	{
		String boardString=white?"w":"b";
		for(char[] a: board)
			boardString+=new String(a);
		return boardString;
	}
	public void move(String m)
	{
		int a='8'-m.charAt(1), b=m.charAt(0)-'a', c='8'-m.charAt(3), d=m.charAt(2)-'a', n=b-d;
		if(m.length()==5)
		{
			total-=values.get(board[a][b]);
			board[a][b]=white?Character.toUpperCase(m.charAt(4)):m.charAt(4);
			total+=values.get(board[a][b]);
		}
		if(c==0)
		{
			if(d==0)
				castle=castle.replace('q', ' ');
			else if(d==7)
				castle=castle.replace('k', ' ');
		}
		else if(c==7)
			if(d==0)
				castle=castle.replace('Q', ' ');
			else if(d==7)
				castle=castle.replace('K', ' ');
		if(board[a][b]=='p'||board[a][b]=='P')
		{
			enPassant=Math.abs(a-c)==2?m:"";
			if(b!=d&&board[c][d]==' ')
			{
				total-=values.get(board[a][d]);
				board[a][d]=' ';
			}
		}
		else
			enPassant="";
		if(board[a][b]=='k')
		{
			blackKing=m.substring(2);
			castle=castle.replace('k', ' ').replace('q', ' ');
			if(n==2)
			{
				board[0][3]='r';
				board[0][0]=' ';
			}
			else if(n==-2)
			{
				board[0][5]='r';
				board[0][7]=' ';
			}
		}
		else if(board[a][b]=='K')
		{
			whiteKing=m.substring(2);
			castle=castle.replace('K', ' ').replace('Q', ' ');
			if(n==2)
			{
				board[7][3]='R';
				board[7][0]=' ';
			}
			else if(n==-2)
			{
				board[7][5]='R';
				board[7][7]=' ';
			}
		}
		else if(a==0)
		{
			if(b==0)
				castle=castle.replace('q', ' ');
			else if(b==7)
				castle=castle.replace('k', ' ');
		}
		else if(a==7)
			if(b==0)
				castle=castle.replace('Q', ' ');
			else if(b==7)
				castle=castle.replace('K', ' ');
		total-=values.get(board[c][d]);
		board[c][d]=board[a][b];
		board[a][b]=' ';
		white=!white;
	}
	private void move(String m, int total)
	{
		move(m);
		char p=board['8'-m.charAt(3)][m.charAt(2)-'a'];
		if(this.total!=total||p=='p'||p=='P')
			positions=new HashMap<>();
		String bs=boardString();
		Integer i=positions.get(bs);
		positions.put(bs, i==null?1:i+1);
		movesPlayed.add(m);
		fixedMoves.add(fixNotation(m));
	}
	private void unmove(String m, String k, String e, char p)
	{
		int a='8'-m.charAt(1), b=m.charAt(0)-'a', c='8'-m.charAt(3), d=m.charAt(2)-'a', n=b-d;
		if(m.length()==5)
		{
			total-=values.get(board[c][d]);
			board[c][d]=white?'p':'P';
			total+=values.get(board[c][d]);
		}
		else if((board[c][d]=='p'||board[c][d]=='P')&&(b!=d&&p==' '))
		{
			total-=values.get(board[c][d]);
			board[a][d]=white?'P':'p';
		}
		else if(board[c][d]=='k')
		{
			blackKing=m.substring(0, 2);
			if(n==2)
			{
				board[0][0]='r';
				board[0][3]=' ';
			}
			else if(n==-2)
			{
				board[0][7]='r';
				board[0][5]=' ';
			}
		}
		else if(board[c][d]=='K')
		{
			whiteKing=m.substring(0, 2);
			if(n==2)
			{
				board[7][0]='R';
				board[7][3]=' ';
			}
			else if(n==-2)
			{
				board[7][7]='R';
				board[7][5]=' ';
			}
		}
		total+=values.get(p);
		castle=k;
		enPassant=e;
		board[a][b]=board[c][d];
		board[c][d]=p;
		white=!white;
	}
	private boolean squareContains(int r, int c, char p)
	{
		return r>=0&&r<8&&c>=0&&c<8&&board[r][c]==p;
	}
	private boolean attacker(int r, int c, int a, int b, char p, char q)
	{
		c+=b;
		for(r+=a; r>=0&&r<8&&c>=0&&c<8; r+=a)
		{
			if(board[r][c]==p||board[r][c]==q)
				return true;
			if(board[r][c]!=' ')
				return false;
			c+=b;
		}
		return false;
	}
	private boolean attackers(boolean w, int r, int c)
	{
		if(w)
		{
			for(int[] m: changes)
				if(squareContains(r+m[0], c+m[1], 'k'))
					return true;
			for(int i=0; i<4; i++)
				if(attacker(r, c, changes[i][0], changes[i][1], 'r', 'q'))
					return true;
			for(int i=4; i<8; i++)
				if(attacker(r, c, changes[i][0], changes[i][1], 'b', 'q'))
					return true;
			for(int[] m: knights)
				if(squareContains(r+m[0], c+m[1], 'n'))
					return true;
			return squareContains(r-1, c-1, 'p')||squareContains(r-1, c+1, 'p');
		}
		for(int[] m: changes)
			if(squareContains(r+m[0], c+m[1], 'K'))
				return true;
		for(int i=0; i<4; i++)
			if(attacker(r, c, changes[i][0], changes[i][1], 'R', 'Q'))
				return true;
		for(int i=4; i<8; i++)
			if(attacker(r, c, changes[i][0], changes[i][1], 'B', 'Q'))
				return true;
		for(int[] m: knights)
			if(squareContains(r+m[0], c+m[1], 'N'))
				return true;
		return squareContains(r+1, c-1, 'P')||squareContains(r+1, c+1, 'P');
	}
	private boolean inCheck(boolean w)
	{
		String k=w?whiteKing:blackKing;
		return attackers(w, '8'-k.charAt(1), k.charAt(0)-'a');
	}
	private boolean add(ArrayList<Move> moves, String m)
	{
		String k=castle, e=enPassant;
		char p=board['8'-m.charAt(3)][m.charAt(2)-'a'];
		move(m);
		boolean b=!inCheck(!white);
		if(b)
			moves.add(new Move(m, total));
		unmove(m, k, e, p);
		return b;
	}
	private void add(ArrayList<Move> m, String s, int r, int c, int a, int b)
	{
		c+=b;
		for(r+=a; r>=0&&r<8&&c>=0&&c<8; r+=a)
		{
			if(board[r][c]==' ')
				add(m, s+stringify(r, c));
			else
			{
				if(Character.isLowerCase(board[r][c])==white)
					add(m, s+stringify(r, c));
				return;
			}
			c+=b;
		}
	}
	public ArrayList<Move> legalMoves()
	{
		ArrayList<Move> m=new ArrayList<>();
		for(int r=0; r<8; r++)
			for(int c=0; c<8; c++)
				if(board[r][c]!=' '&&Character.isUpperCase(board[r][c])==white)
				{
					String s=stringify(r, c);
					switch(Character.toLowerCase(board[r][c]))
					{
						case 'r':
							for(int i=0; i<4; i++)
								add(m, s, r, c, changes[i][0], changes[i][1]);
							break;
						case 'b':
							for(int i=4; i<8; i++)
								add(m, s, r, c, changes[i][0], changes[i][1]);
							break;
						case 'q':
							for(int i=0; i<8; i++)
								add(m, s, r, c, changes[i][0], changes[i][1]);
							break;
						case 'n':
							for(int[] a: knights)
							{
								int x=r+a[0], y=c+a[1];
								if(x>=0&&x<8&&y>=0&&y<8&&(board[x][y]==' '||Character.isLowerCase(board[x][y])==white))
									add(m, s+stringify(x, y));
							}
							break;
						case 'k':
							for(int[] a: changes)
							{
								int x=r+a[0], y=c+a[1];
								if(x>=0&&x<8&&y>=0&&y<8&&(board[x][y]==' '||Character.isLowerCase(board[x][y])==white))
									add(m, s+stringify(x, y));
							}
							if(!attackers(white, r, c))
							{
								if(white)
								{
									if(board[7][5]==' '&&board[7][6]==' '&&castle.indexOf("K")>=0&&!attackers(white, 7, 5))
										add(m, "e1g1");
									if(board[7][1]==' '&&board[7][2]==' '&&board[7][3]==' '&&castle.indexOf("Q")>=0&&!attackers(white, 7, 3))
										add(m, "e1c1");
								}
								else
								{
									if(board[0][5]==' '&&board[0][6]==' '&&castle.indexOf("k")>=0&&!attackers(white, 0, 5))
										add(m, "e8g8");
									if(board[0][1]==' '&&board[0][2]==' '&&board[0][3]==' '&&castle.indexOf("q")>=0&&!attackers(white, 0, 3))
										add(m, "e8c8");
								}
							}
							break;
						default:
							if(white)
							{
								String q=r==1?"q":"";
								if(enPassant.length()==4&&s.charAt(1)==enPassant.charAt(3)&&Math.abs(s.charAt(0)-enPassant.charAt(2))==1)
									add(m, s+enPassant.charAt(2)+(char)(enPassant.charAt(3)+1));
								if(board[r-1][c]==' '&&add(m, s+stringify(r-1, c)+q)&&r==6&&board[4][c]==' ')
									add(m, s+s.charAt(0)+4);
								if(c>0&&Character.isLowerCase(board[r-1][c-1]))
									add(m, s+stringify(r-1, c-1)+q);
								if(c<7&&Character.isLowerCase(board[r-1][c+1]))
									add(m, s+stringify(r-1, c+1)+q);
							}
							else
							{
								String q=r==6?"q":"";
								if(enPassant.length()==4&&s.charAt(1)==enPassant.charAt(3)&&Math.abs(s.charAt(0)-enPassant.charAt(2))==1)
									add(m, s+enPassant.charAt(2)+(char)(enPassant.charAt(3)-1));
								if(board[r+1][c]==' '&&add(m, s+stringify(r+1, c)+q)&&r==1&&board[3][c]==' ')
									add(m, s+s.charAt(0)+5);
								if(c>0&&board[r+1][c-1]!=' '&&!Character.isLowerCase(board[r+1][c-1]))
									add(m, s+stringify(r+1, c-1)+q);
								if(c<7&&board[r+1][c+1]!=' '&&!Character.isLowerCase(board[r+1][c+1]))
									add(m, s+stringify(r+1, c+1)+q);
							}
					}
				}
		Collections.sort(m);
		return m;
	}
	private class Move implements Comparable<Move>
	{
		private String move;
		private int evaluation, value, numMoves, captures;
		private boolean check, castle;
		private Move next;
		public Move()
		{
			evaluation=white?-300:300;
		}
		public Move(int evaluation)
		{
			this.evaluation=evaluation;
		}
		private void addMoves(int r, int c, int a, int b, int value)
		{
			c+=b;
			for(r+=a; r>=0&&r<8&&c>=0&&c<8; r+=a)
			{
				if(board[r][c]==' ')
					numMoves++;
				else
				{
					if(Character.isUpperCase(board[r][c])==white)
					{
						numMoves++;
						int v=Math.abs(values.get(board[r][c]));
						if(value<v)
							captures+=v;
					}
					return;
				}
				c+=b;
			}
		}
		public Move(String move, int evaluation)
		{
			this.move=move;
			this.evaluation=evaluation;
			char t=move.charAt(2);
			value=Math.abs(values.get(board['8'-move.charAt(3)][t-'a']));
			castle=value==300&&move.charAt(0)=='e'&&(t=='c'||t=='g');
			numMoves=0;
			captures=0;
			for(int r=0; r<8; r++)
				for(int c=0; c<8; c++)
					if(board[r][c]!=' '&&Character.isLowerCase(board[r][c])==white)
						switch(Character.toLowerCase(board[r][c]))
						{
							case 'p':
								if(white)
								{
									if(c>0)
									{
										int v=values.get(board[r+1][c-1]);
										if(v>1)
											captures+=v;
									}
									if(c<7)
									{
										int v=values.get(board[r+1][c+1]);
										if(v>1)
											captures+=v;
									}
								}
								else
								{
									if(c>0)
									{
										int v=values.get(board[r-1][c-1]);
										if(v<-1)
											captures-=v;
									}
									if(c<7)
									{
										int v=values.get(board[r-1][c+1]);
										if(v<-1)
											captures-=v;
									}
								}
								break;
							case 'r':
								for(int i=0; i<4; i++)
									addMoves(r, c, changes[i][0], changes[i][1], 5);
								break;
							case 'b':
								for(int i=4; i<8; i++)
									addMoves(r, c, changes[i][0], changes[i][1], 3);
								break;
							case 'n':
								for(int[] a: knights)
								{
									int x=r+a[0], y=c+a[1];
									if(x>=0&&x<8&&y>=0&&y<8)
										if(board[x][y]==' ')
											numMoves++;
										else if(Character.isUpperCase(board[x][y])==white)
										{
											numMoves++;
											int v=Math.abs(values.get(board[x][y]));
											if(3<v)
												captures+=v;
										}
								}
								break;
						}
			check=inCheck(white);
		}
		public void evaluate(String move, Move next)
		{
			if(this.move==null||(white?evaluation>next.evaluation:evaluation<next.evaluation))
			{
				this.move=move;
				evaluation=next.evaluation;
				this.next=next;
			}
		}
		public int compareTo(Move m)
		{
			return check==m.check?evaluation==m.evaluation?castle==m.castle?captures==m.captures?numMoves==m.numMoves?
					value-m.value:m.numMoves-numMoves:m.captures-captures:castle?-1:1:white?m.evaluation-evaluation:evaluation-m.evaluation:check?-1:1;
		}
		public String toString()
		{
			return move+(next.move==null?": "+next.evaluation:"->"+next);
		}
	}
	private Move bestCapture(Move b, boolean inCheck, ArrayList<Move> moves, int alpha, int beta)
	{
		if(moves.isEmpty())
			return inCheck?new Move():new Move(0);
		String k=castle, e=enPassant;
		b.move="";
		for(Move m: moves)
		{
			if(m.evaluation==total)
				if(m.check)
					continue;
				else
					break;
			char p=board['8'-m.move.charAt(3)][m.move.charAt(2)-'a'];
			move(m.move);
			String bs=boardString();
			Integer n=checkedBoards.get(bs);
			if(n==null)
			{
				Move r=bestCapture(new Move(total), m.check, legalMoves(), alpha, beta);
				checkedBoards.put(bs, r.evaluation);
				b.evaluate(m.move, r);
			}
			else
				b.evaluate(m.move, new Move(n));
			unmove(m.move, k, e, p);
			if(white)
				alpha = Math.max(alpha, b.evaluation);
			else
				beta = Math.min(beta, b.evaluation);
			if (beta <= alpha)
				return b;
		}
		if(b.move.isEmpty())
			b.move=null;
		return b;
	}
	private void evaluate(String m, Move b, int d, String k, String e, boolean check, int alpha, int beta)
	{
		char p=board['8'-m.charAt(3)][m.charAt(2)-'a'];
		move(m);
		String bs=boardString();
		Integer n=checkedBoards.get(bs);
		if(n==null)
		{
			Move r=bestMove(d, check, alpha, beta);
			checkedBoards.put(bs, r.evaluation);
			b.evaluate(m, r);
		}
		else
			b.evaluate(m, new Move(n));
		unmove(m, k, e, p);
	}
	private Move bestMove(int d, boolean inCheck, int alpha, int beta)
	{
		Move b=new Move();
		ArrayList<Move> moves=legalMoves();
		if(moves.isEmpty())
			return inCheck?b:new Move(0);
		String k=castle, e=enPassant;
		if(d--==0)
			return bestCapture(new Move(total), inCheck, moves, alpha, beta);
		for(int i=0; i<2; i++)
			for(int j=0; j<moves.size(); j++)
			{
				String m=moves.get(j).move;
				if(m.equals(killerMoves[d][i]))
				{
					evaluate(m, b, d, k, e, moves.get(j).check, alpha, beta);
					if(engineWhite?white&&b.evaluation>=total:!white&&b.evaluation<=total)
						return b;
					if (white)
						alpha = Math.max(alpha, b.evaluation);
					else
						beta = Math.min(beta, b.evaluation);
					if (beta <= alpha)
						return b;
					moves.remove(j);
					break;
				}
			}
		for(Move m: moves)
		{
			evaluate(m.move, b, d, k, e, m.check, alpha, beta);
			if(engineWhite?white&&b.evaluation>=total:!white&&b.evaluation<=total)
				return b;
			if (white)
				alpha = Math.max(alpha, b.evaluation);
			else
				beta = Math.min(beta, b.evaluation);
			if (beta <= alpha)
			{
				killerMoves[d][1]=killerMoves[d][0];
				killerMoves[d][0]=m.move;
				break;
			}
		}
		return b;
	}
	public Move bestMove(int d) {
		Move b = new Move();
		ArrayList<Move> moves = legalMoves();
		if (moves.isEmpty())
			return inCheck(white)?b:new Move(0);
		checkedBoards=new ConcurrentHashMap<>();
		final int depth=d-1;
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<Future<Move>> futures = new ArrayList<>();
		
		for (Move m : moves) {
			futures.add(executor.submit(() -> {
				Chess chess=new Chess(this);
				chess.move(m.move);
				Move evaluatedMove = new Move();
				evaluatedMove.evaluate(m.move, positions.get(chess.boardString())==null?chess.bestMove(depth, m.check, -300, 300):new Move(0));
				return evaluatedMove;
			}));
		}
		
		executor.shutdown();
		
		for (Future<Move> future: futures) {
			try {
				Move result = future.get();
				if (b.move==null||(white?result.evaluation>b.evaluation:result.evaluation<b.evaluation))
					b = result;
			} catch (InterruptedException | ExecutionException ex) {
				ex.printStackTrace();
			}
		}
		return b;
	}
	public String fixNotation(String m)
	{
		int c='8'-m.charAt(3), d=m.charAt(2)-'a';
		return board[c][d]=='k'||board[c][d]=='K'?m.equals("e8c8")||m.equals("e1c1")?"O-O-O":m.equals("e8g8")||m.equals("e1g1")?"O-O":"K"+m.substring(2):
		m.length()==5?m.substring(2, 4)+"="+board[c][d]:board[c][d]=='p'||board[c][d]=='P'?m:Character.toUpperCase(board[c][d])+m;
	}
	public void moveAndPrint(Move m)
	{
		move(m.move, total);
		System.out.println(m);
	}
	public void playStockfishMove()
	{
		try
		{
            Process process = new ProcessBuilder("python", "lifish.py", String.join(",", movesPlayed)).start();

            // Capture output from Python script
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
			String m=reader.readLine();
			System.out.println(m);
			move(m, total);

            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	public void printPgn(String s)
	{
		for(int i=0; i<movesPlayed.size(); i++)
		{
			System.out.print(" "+(i/2+1)+". "+fixedMoves.get(i++));
			if(i<movesPlayed.size())
				System.out.print(" "+fixedMoves.get(i));
		}
		System.out.println("\n"+s);
	}
	private static String reverse(String s)
	{
		return new StringBuilder(s).reverse().toString();
	}
	public String toString()
	{
		String r=lower;
		for(int i=0; i<8; i++)
			r+=8-i+" |"+String.join("|", new String(board[i]).split(""))+"| "+(8-i)+dashes;
		r+=upper;
		return white?r:reverse(r);
	}
	public static boolean isIllegal(ArrayList<Move> moves, String move)
	{
		for(Move m: moves)
			if(m.move.equals(move))
				return false;
		return true;
	}
	public static void main(String[] a)
	{
		//"7k/8/8/8/8/8/p7/7K w - -"
		//FEN w/b castle enPassant
		int d=0;
		Move move;
		try
		{
			d=Integer.parseInt(a[0]);
		}
		catch(Exception e)
		{
			System.out.println("Usage: java Chess depth firstMove (Depth must be greater than 0, firstMove is optional)");
			return;
		}
		Chess chess=new Chess(d);
		//Chess chess=new Chess("6k1/2b2np1/p2q1p1p/Prp1rP1P/1p1p4/1P1P4/Q1PBB1P1/R1K2R2 w - - 0 37", 3);
		Scanner input=new Scanner(System.in);
		if(a.length==2&&!isIllegal(chess.legalMoves(), a[1]))
		{
			engineWhite=false;
			chess.move(a[1], chess.total);
		}
		System.out.println(chess);
		chess.moveAndPrint(chess.bestMove(d));
		while(true)
		{
			ArrayList<Move> moves=chess.legalMoves();
			System.out.println(chess);
			if(moves.isEmpty())
			{
				chess.printPgn("I win!");
				return;
			}
			/*String m=input.nextLine();
			while(isIllegal(moves, m))
			{
				System.out.println("That is an invalid move... but you could try "+moves.get(0).move);
				m=input.nextLine();
			}
			chess.move(m, chess.total);*/
			chess.playStockfishMove();
			for(int i=0; i<d; i++)
				for(int j=0; j<2; j++)
					chess.killerMoves[i][j]="";
			System.out.println(chess);
			move=chess.bestMove(d);
			//move=chess.bestMove(d, chess.inCheck(chess.white), -300, 300);
			if(move.move==null)
			{
				chess.printPgn(move.evaluation==0?"Draw!":"I lose!");
				return;
			}
			chess.moveAndPrint(move);
		}
	}
}