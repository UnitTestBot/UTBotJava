class NoTestsProblem:
    def __init__(self):
        self.board = []

    def set_position(self, row, col, symbol):
        self.board[row][col] = symbol
        return symbol

    def start(self):
        self.set_position(1, 2, "O")
