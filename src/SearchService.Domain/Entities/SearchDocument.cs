namespace SearchService.Domain.Entities;

/// <summary>
/// Representa el documento derivado optimizado para indexación y búsqueda rápida (Read Model en CQRS).
/// Esta entidad no refleja datos transaccionales crudos de forma directa.
/// </summary>
public class SearchDocument
{
    public string ProductId { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string Category { get; set; } = string.Empty;
    public decimal Price { get; set; }
    public double Rating { get; set; }
    public bool Available { get; set; }
    public string Brand { get; set; } = string.Empty;
}