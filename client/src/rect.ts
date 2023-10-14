class Rectangle {
  constructor(public x: number, public y: number, public width: number, public height: number) {}

  containsPoint(x: number, y: number): boolean {
    return x >= this.x && x <= this.x + this.width && y >= this.y && y <= this.y + this.height;
  }
}

export default Rectangle;
