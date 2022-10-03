package me.dio.sacola.service.impl;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import me.dio.sacola.enumeration.FormaPagamento;
import me.dio.sacola.model.Item;
import me.dio.sacola.model.Restaurante;
import me.dio.sacola.model.Sacola;
import me.dio.sacola.repository.ItemRepository;
import me.dio.sacola.repository.ProdutoRepository;
import me.dio.sacola.repository.SacolaRepository;
import me.dio.sacola.resource.dto.ItemDto;
import me.dio.sacola.service.SacolaService;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SacolaServiceImpl implements SacolaService {
    private final SacolaRepository sacolaRepository;
    private final ProdutoRepository produtoRepository;
    private final ItemRepository itemRepository;
    @Override
    public Item incluirItemNaSacola(ItemDto itemDto) {
        Sacola sacola = verSacola(itemDto.getSacolaId());

        if (sacola.isFechada()){
            throw new RuntimeException("esta sacola esta fechada");
        }

        Item itemParaSerInserido = Item.builder()
                .quantidade(itemDto.getQuantidade())
                .sacola(sacola)
                .produto(produtoRepository.findById(itemDto.getProdutoId()).orElseThrow(
                        () -> {
                            throw new RuntimeException("Esse produto nao existe!");
                        }
                ))
                .build();

        List<Item> itensDaSacola = sacola.getItens();
        if (itensDaSacola.isEmpty()) {
            itensDaSacola.add(itemParaSerInserido);
        } else {
           Restaurante restauranteAtual = itensDaSacola.get(0).getProduto().getRestaurante();
           Restaurante restauranteDoItemParaAdicionar = itemParaSerInserido.getProduto().getRestaurante();
           if (restauranteAtual.equals(restauranteDoItemParaAdicionar)) {
               itensDaSacola.add(itemParaSerInserido);
           } else {
               throw new RuntimeException("Nao é possivel adicionar produtos de restaurantes diferentes. Feche a sacola ou esvazie.");
           }
        }
        List<Double> valorDosItens = new ArrayList<>();
        for (Item itemDaSacola: itensDaSacola) {
            double valorTotalItem = itemDaSacola.getProduto().getValorUnitario() * itemDaSacola.getQuantidade();
        valorDosItens.add(valorTotalItem);
        }

        double valorTotalSacola = valorDosItens.stream()
                        .mapToDouble(valorTotaldeCadaItem -> valorTotaldeCadaItem)
                .sum();


        sacola.setValorTotal(valorTotalSacola);
        sacolaRepository.save(sacola);
        return (itemParaSerInserido);
    }

    @Override
    public Sacola verSacola(long id) {
        return sacolaRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Essa sacola não existe!")
        );
    }

    @Override
    public Sacola fecharSacola(long id, int numeroformaPagamento) {
        Sacola sacola  = verSacola(id);
        if (sacola.getItens().isEmpty()) {
            throw new RuntimeException("Inclua Itens na sacola");
        }


        FormaPagamento formaPagamento =
                numeroformaPagamento == 0 ? FormaPagamento.DINHEIRO : FormaPagamento.MAQUINETA;

        sacola.setFormaPagamento(formaPagamento);
        sacola.setFechada(true);
        return sacolaRepository.save(sacola);
    }
}
