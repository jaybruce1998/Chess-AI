import sys
from stockfish import Stockfish
#[5, 5, 5, 5, 5, 8, 13, 22]
#[-9, -5, -1, 3, 7, 11, 16, 20]
#[50, 100, 150, 200, 300, 400, 500, 1000]
stockfish = Stockfish(path="stockfish", depth=5, parameters={"Skill Level": 7, "Slow Mover": 300})
stockfish.set_position(sys.argv[1].split(","))
print(stockfish.get_best_move())