import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Scanner;
public class Chess
{
	private final String dashes="\n  -----------------  \n", lower="\n   a b c d e f g h   "+dashes, upper="   A B C D E F G H   \n";
	private final int[][] changes={{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}}, knights={{-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}};
	private char[][] board;
	private boolean white;
	private String castle, enPassant, whiteKing, blackKing;
	private HashMap<Character, Integer> values;
	private int total;
	private String stringify(int r, int c)
	{
		return ""+(char)('a'+c)+(8-r);
	}
	public Chess(String fen)
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
		values=new HashMap<>();
		values.put(' ', 0);
		values.put('r', -5);
		values.put('n', -3);
		values.put('b', -3);
		values.put('q', -9);
		values.put('k', -300);
		values.put('p', -1);
		values.put('R', 5);
		values.put('N', 3);
		values.put('B', 3);
		values.put('Q', 9);
		values.put('K', 300);
		values.put('P', 1);
		total=0;
		for(int i=0; i<8; i++)
			for(int j=0; j<8; j++)
				total+=values.get(board[i][j]);
	}
	public Chess()
	{
		this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -");
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
			if(Math.abs(a-c)==2)
				enPassant=m;
			else
				enPassant="";
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
	private boolean add(ArrayList<String> moves, String m)
	{
		String k=castle, e=enPassant;
		char p=board['8'-m.charAt(3)][m.charAt(2)-'a'];
		int r, c;
		move(m);
		if(white)
		{
			r='8'-blackKing.charAt(1);
			c=blackKing.charAt(0)-'a';
		}
		else
		{
			r='8'-whiteKing.charAt(1);
			c=whiteKing.charAt(0)-'a';
		}
		boolean b=!attackers(!white, r, c);
		unmove(m, k, e, p);
		if(b)
			moves.add(m);
		return b;
	}
	private void add(ArrayList<String> m, String s, int r, int c, int a, int b)
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
	public ArrayList<String> legalMoves()
	{
		ArrayList<String> m=new ArrayList<>();
		for(int r=0; r<8; r++)
			for(int c=0; c<8; c++)
				if(board[r][c]!=' '&&Character.isUpperCase(board[r][c])==white)
				{
					char p=Character.toLowerCase(board[r][c]);
					String s=stringify(r, c);
					if(p=='r')
						for(int i=0; i<4; i++)
							add(m, s, r, c, changes[i][0], changes[i][1]);
					else if(p=='b')
						for(int i=4; i<8; i++)
							add(m, s, r, c, changes[i][0], changes[i][1]);
					else if(p=='q')
						for(int i=0; i<8; i++)
							add(m, s, r, c, changes[i][0], changes[i][1]);
					else if(p=='n')
						for(int[] a: knights)
						{
							int x=r+a[0], y=c+a[1];
							if(x>=0&&x<8&&y>=0&&y<8&&(board[x][y]==' '||Character.isLowerCase(board[x][y])==white))
								add(m, s+stringify(x, y));
						}
					else if(p=='k')
					{
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
					}
					else
					{
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
		return m;
	}
	private class Move
	{
		private String move;
		private int evaluation;
		private Move next;
		public Move()
		{
			evaluation=white?-300:300;
		}
		public Move(int evaluation)
		{
			this.evaluation=evaluation;
		}
		public void evaluate(String move, Move next)
		{
			if(this.move==null||white&&evaluation>next.evaluation||!white&&evaluation<next.evaluation||evaluation==next.evaluation&&Math.random()<0.125)
			{
				this.move=move;
				evaluation=next.evaluation;
				this.next=next;
			}
		}
		public String toString()
		{
			return move==null?"Final Evaluation: "+evaluation:move+"->"+next;
		}
	}
	public Move bestMove(int d)
	{
		Move r=new Move();
		String k=white?whiteKing:blackKing, e;
		char p;
		ArrayList<String> moves=legalMoves();
		if(moves.size()==0)
			return attackers(white, '8'-k.charAt(1), k.charAt(0)-'a')?r:new Move(0);
		if(d==0)
			return new Move(total);
		for(String m: moves)
		{
			k=castle;
			e=enPassant;
			p=board['8'-m.charAt(3)][m.charAt(2)-'a'];
			move(m);
			r.evaluate(m, bestMove(d-1));
			unmove(m, k, e, p);
		}
		return r;
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
	public String fixNotation(String m)
	{
		int c='8'-m.charAt(3), d=m.charAt(2)-'a';
		return board[c][d]=='k'||board[c][d]=='K'?m.equals("e8c8")||m.equals("e1c1")?"O-O-O":m.equals("e8g8")||m.equals("e1g1")?"O-O":"K"+m.substring(2):
		board[c][d]=='p'||board[c][d]=='P'?m:Character.toUpperCase(board[c][d])+m;
	}
	public static void main(String[] a)
	{
		//"7k/8/8/8/8/8/p7/7K w - -"
		//FEN w/b castle enPassant
		//Chess chess=new Chess(reverse("R1BK3R/PPP1NQPP/2NB4/3P1P2/b7/1p2p3/p1ppnpr1/rnbkq3")+" b KQk -");
		Chess chess=new Chess();
		Scanner input=new Scanner(System.in);
		int d=Integer.parseInt(a[0]);
		while(true)
		{
			System.out.println(chess.castle+" "+chess.enPassant);
			ArrayList<String> moves=chess.legalMoves();
			System.out.println(chess);
			String m=input.nextLine();
			while(!moves.contains(m))
			{
				System.out.println("That is an invalid move... but you could try "+moves.get(0));
				m=input.nextLine();
			}
			chess.move(m);
			System.out.println(chess);
			Move move=chess.bestMove(d);
			System.out.println(move);
			chess.move(move.move);
		}
	}
}