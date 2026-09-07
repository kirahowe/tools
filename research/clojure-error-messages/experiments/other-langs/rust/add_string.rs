fn total(prices: &[i32]) -> i32 {
    prices.iter().sum()
}

fn main() {
    let t = 1 + "2";
    println!("{}", total(&[10, 20]) + t);
}
